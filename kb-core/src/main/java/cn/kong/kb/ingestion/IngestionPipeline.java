package cn.kong.kb.ingestion;

import cn.kong.kb.domain.DocumentStatus;
import cn.kong.kb.domain.KbChunk;
import cn.kong.kb.domain.KbDocumentType;
import cn.kong.kb.ingestion.chunker.ChunkerFactory;
import cn.kong.kb.ingestion.parser.DocumentParser;
import cn.kong.kb.ingestion.polish.ChunkPolishService;
import cn.kong.kb.mapper.ChunkMapper;
import cn.kong.kb.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 异步文档摄入流水线：解析 → 分块 → 润色 → 向量化入库 → 状态回写。
 *
 * <p><b>为什么是独立组件：</b>{@code @Async} 基于 Spring AOP 代理，只拦截
 * 从外部进入的调用；若与 {@link DocumentIngestionService} 合并在同一个类里，
 * 同类内部调用（{@code this.xxx}）会绕过代理，异步注解完全失效——
 * 整条流水线将退化为在 HTTP 请求线程中同步执行。拆分后
 * {@code DocumentIngestionService#ingestDocument} 经由本 Bean 触发
 * {@link #process}，代理生效，流水线运行在 {@code ingestionExecutor} 上。</p>
 *
 * <p><b>切片主键：</b>切片 ID 由 {@link KbChunk} 构造时预先生成，并作为
 * Spring AI {@link Document} 的 ID 传入 PgVectorStore，向量表主键与业务切片
 * ID 天然一致，无需事后回写，也无需全局串行化。</p>
 */
@Component
public class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    /** 每批向量化的切片数，与 PgVectorStore 的 max-document-batch-size 保持一致 */
    private static final int EMBEDDING_BATCH_SIZE = 20;

    private final DocumentParser documentParser;
    private final ChunkerFactory chunkerFactory;
    private final ChunkPolishService chunkPolishService;
    private final PgVectorStore vectorStore;
    private final ChunkMapper chunkMapper;
    private final DocumentMapper documentMapper;

    public IngestionPipeline(DocumentParser documentParser,
                             ChunkerFactory chunkerFactory,
                             ChunkPolishService chunkPolishService,
                             PgVectorStore vectorStore,
                             ChunkMapper chunkMapper,
                             DocumentMapper documentMapper) {
        this.documentParser = documentParser;
        this.chunkerFactory = chunkerFactory;
        this.chunkPolishService = chunkPolishService;
        this.vectorStore = vectorStore;
        this.chunkMapper = chunkMapper;
        this.documentMapper = documentMapper;
    }

    /**
     * 异步处理单个文档。
     *
     * <p>任何一步失败都会将文档状态置为 FAILED 并记录原因，
     * 不向调用方抛出（void 异步方法）。</p>
     */
    @Async("ingestionExecutor")
    public void process(MultipartFile file, UUID documentId, KbDocumentType type) {
        try {
            log.info("开始异步处理文档 {}", documentId);

            // 第一步：解析
            List<Document> parsedDocs = documentParser.parse(file);
            log.info("从文档 {} 解析出 {} 个片段", documentId, parsedDocs.size());

            // 第二步：分块
            List<KbChunk> kbChunks = chunkerFactory.chunk(parsedDocs, documentId, type);
            log.info("为文档 {} 生成 {} 个切片", documentId, kbChunks.size());

            // 第三步：AI 润色
            chunkPolishService.polishChunks(kbChunks);

            // 第四步：向量化并入库（分批进行）
            int totalBatches = (int) Math.ceil((double) kbChunks.size() / EMBEDDING_BATCH_SIZE);
            for (int i = 0; i < kbChunks.size(); i += EMBEDDING_BATCH_SIZE) {
                int end = Math.min(i + EMBEDDING_BATCH_SIZE, kbChunks.size());
                vectorizeAndStore(kbChunks.subList(i, end));
                log.info("已存储文档 {} 的第 {}/{} 批", documentId,
                        i / EMBEDDING_BATCH_SIZE + 1, totalBatches);
            }

            // 第五步：更新文档状态
            documentMapper.updateChunkCount(documentId, kbChunks.size());
            documentMapper.updateStatus(documentId, DocumentStatus.COMPLETED, null);
            log.info("文档 {} 处理完成，共 {} 个切片", documentId, kbChunks.size());

        } catch (Exception e) {
            log.error("处理文档 {} 失败", documentId, e);
            documentMapper.updateStatus(documentId, DocumentStatus.FAILED, e.getMessage());
        }
    }

    /**
     * 将一批切片交给 PgVectorStore 向量化并插入 kb_chunks，
     * 随后以单条 UPDATE 批量填充业务列（document_id / chunk_index / chunk_type / title）。
     *
     * <p>切片 ID 已预设在 Spring AI Document 上，向量插入与业务列填充
     * 由同一主键关联，无需逐条回写，也无需全局串行化。
     * 若业务列填充失败，尝试删除本批已插入的行，避免产生不可见的孤儿切片。</p>
     */
    private void vectorizeAndStore(List<KbChunk> batch) {
        List<Document> aiDocs = batch.stream()
                .map(chunk -> Document.builder()
                        .id(chunk.getId().toString())
                        .text(chunk.getContent())
                        .metadata(chunk.getMetadata() != null ? chunk.getMetadata() : Map.of())
                        .build())
                .toList();

        vectorStore.add(aiDocs);

        try {
            chunkMapper.fillChunkMetadata(batch);
        } catch (Exception e) {
            log.error("业务列批量填充失败，尝试补偿删除 {} 个切片", batch.size(), e);
            try {
                chunkMapper.deleteByIds(batch.stream().map(KbChunk::getId).toList());
            } catch (Exception suppressed) {
                log.error("补偿删除失败，可能残留 {} 个缺失业务列的切片，请按主键清理", batch.size(), suppressed);
            }
            throw e;
        }
    }
}
