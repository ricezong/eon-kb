package cn.kong.kb.ingestion;

import cn.kong.kb.domain.KbDocument;
import cn.kong.kb.domain.KbDocumentType;
import cn.kong.kb.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 文档摄入的同步入口：创建文档记录，然后把重活交给异步流水线。
 *
 * <p>异步处理在 {@link IngestionPipeline} 中执行。拆分为独立 Bean 的原因
 * 见其 Javadoc——同类内 this 调用会绕过 {@code @Async} 代理，
 * 导致"异步摄入"实际同步阻塞 HTTP 请求线程。</p>
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final DocumentMapper documentMapper;
    private final IngestionPipeline ingestionPipeline;

    public DocumentIngestionService(DocumentMapper documentMapper,
                                    IngestionPipeline ingestionPipeline) {
        this.documentMapper = documentMapper;
        this.ingestionPipeline = ingestionPipeline;
    }

    /**
     * 同步入口：保存文档记录（状态 PROCESSING）并触发异步处理。
     *
     * @param file 上传的文件
     * @return 新建文档的 ID
     */
    public UUID ingestDocument(MultipartFile file) {
        KbDocumentType type = KbDocumentType.fromFileName(file.getOriginalFilename());

        KbDocument kbDocument = new KbDocument(
                file.getOriginalFilename(),
                type.name(),
                file.getSize()
        );

        // 插入文档记录，主键通过 useGeneratedKeys 回写到 kbDocument.id
        documentMapper.insert(kbDocument);
        UUID documentId = kbDocument.getId();
        log.info("文档记录已创建：{}（{}）", file.getOriginalFilename(), documentId);

        // 经由独立 Bean 触发异步流水线（跨 Bean 调用才能命中 @Async 代理）
        ingestionPipeline.process(file, documentId, type);

        return documentId;
    }
}
