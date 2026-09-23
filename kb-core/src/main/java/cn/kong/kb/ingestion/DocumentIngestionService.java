package cn.kong.kb.ingestion;

import cn.kong.kb.domain.KbDocument;
import cn.kong.kb.domain.KbDocumentType;
import cn.kong.kb.domain.ParseMode;
import cn.kong.kb.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
     * @param source 文档输入快照（已落盘，生命周期独立于 HTTP 请求，可安全跨线程传递）
     * @param mode   解析方式（AUTO / LOCAL / CLOUD），随文档持久化并传给解析路由
     * @return 新建文档的 ID
     */
    public UUID ingestDocument(DocumentSource source, ParseMode mode) {
        KbDocumentType type = KbDocumentType.fromFileName(source.filename());

        KbDocument kbDocument = new KbDocument(
                source.filename(),
                type.name(),
                source.size()
        );
        kbDocument.setParseMode(mode);

        // 插入文档记录，主键通过 useGeneratedKeys 回写到 kbDocument.id
        documentMapper.insert(kbDocument);
        UUID documentId = kbDocument.getId();
        log.info("文档记录已创建：{}（{}，解析方式 {}）", source.filename(), documentId, mode);

        // 经由独立 Bean 触发异步流水线（跨 Bean 调用才能命中 @Async 代理）
        ingestionPipeline.process(source, documentId, type, mode);

        return documentId;
    }
}
