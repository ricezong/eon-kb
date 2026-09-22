package cn.kong.kb.tool;

import cn.kong.kb.domain.KbChunk;
import cn.kong.kb.mapper.ChunkMapper;
import cn.kong.kb.rerank.RerankService;
import cn.kong.kb.retrieval.HybridSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@link KnowledgeBaseTool} 的默认实现：复用现有检索链路，
 * 编排"混合检索（三路并行 + RRF 融合）→ 重排"，并将结果映射为稳定的 DTO。
 *
 * <p>与 {@code ChatService} 的区别：<b>不做 LLM 答案生成</b>，
 * 只返回带来源的原文片段，生成环节交给调用方 agent。</p>
 *
 * <p>方法上的 {@code @Tool} / {@code @ToolParam} 注解使其可被 Spring AI
 * function-calling 直接注册（如 {@code ToolCallbacks.from(bean)} 或
 * {@code ChatClient...tools(bean)}）。</p>
 */
@Component
public class DefaultKnowledgeBaseTool implements KnowledgeBaseTool {

    private static final Logger log = LoggerFactory.getLogger(DefaultKnowledgeBaseTool.class);

    private final HybridSearchService hybridSearchService;
    private final RerankService rerankService;
    private final ChunkMapper chunkMapper;

    @Value("${app.rerank.top-n:5}")
    private int defaultTopK;

    public DefaultKnowledgeBaseTool(HybridSearchService hybridSearchService,
                                    RerankService rerankService,
                                    ChunkMapper chunkMapper) {
        this.hybridSearchService = hybridSearchService;
        this.rerankService = rerankService;
        this.chunkMapper = chunkMapper;
    }

    @Override
    @Tool(name = "search_knowledge_base",
            description = "在 SuperBrain 知识库中检索与问题相关的文档片段，返回带来源（文档名/标题）的原文摘录，"
                    + "按相关性降序排列。当用户问题涉及内部资料、文档或知识库内容时调用此工具。"
                    + "本工具只做检索、不生成答案，请依据返回的片段自行组织回答并标注来源。")
    public RetrievalResult search(
            @ToolParam(description = "用户问题或检索关键词") String query,
            @ToolParam(description = "限定的知识库 ID 列表；留空表示在所有知识库中检索", required = false)
            List<String> knowledgeBaseIds,
            @ToolParam(description = "返回片段数上限，默认 5", required = false) Integer topK) {

        if (query == null || query.isBlank()) {
            return RetrievalResult.empty(query);
        }
        int k = (topK != null && topK > 0) ? topK : defaultTopK;
        List<UUID> kbUuids = parseIds(knowledgeBaseIds);

        log.info("知识库工具检索：query='{}', kbIds={}, topK={}", query, kbUuids, k);

        // 第一步：混合检索（向量 / 全文 / 模糊 三路并行 + RRF 融合）
        List<KbChunk> candidates = hybridSearchService.search(query, kbUuids);
        if (candidates.isEmpty()) {
            log.info("知识库工具检索无命中");
            return RetrievalResult.empty(query);
        }

        // 第二步：重排，保留 Top-K
        List<KbChunk> reranked = rerankService.rerank(query, candidates, k);

        // 第三步：映射为对外 DTO（补充文档名等来源信息）
        List<RetrievedChunk> chunks = new ArrayList<>(reranked.size());
        for (KbChunk c : reranked) {
            chunks.add(toDto(c));
        }

        log.info("知识库工具检索返回 {} 个片段", chunks.size());
        return new RetrievalResult(query, chunks.size(), chunks);
    }

    private RetrievedChunk toDto(KbChunk c) {
        String documentName = chunkMapper.findDocumentNameByChunkId(c.getId());
        return new RetrievedChunk(
                c.getId() != null ? c.getId().toString() : null,
                c.getDocumentId() != null ? c.getDocumentId().toString() : null,
                documentName != null ? documentName : "Unknown",
                c.getTitle(),
                c.getChunkType(),
                c.getContent(),
                c.getScore());
    }

    /**
     * 将字符串形式的知识库 ID 解析为 UUID；非法项忽略，空列表返回 null（表示全局检索）。
     */
    private List<UUID> parseIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        List<UUID> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            try {
                result.add(UUID.fromString(id.trim()));
            } catch (IllegalArgumentException e) {
                log.warn("忽略非法知识库 ID：{}", id);
            }
        }
        return result.isEmpty() ? null : result;
    }
}
