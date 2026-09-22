package cn.kong.kb.ingestion.parser;

import cn.kong.kb.domain.KbDocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档解析路由器——摄入流水线实际注入的 {@code @Primary} 解析器。
 *
 * <p>路由规则：</p>
 * <ul>
 *   <li>HTML → {@link HtmlDocumentParser}（本地，jsoup + flexmark 转 Markdown）；</li>
 *   <li>配置的云端类型（默认 PDF/DOCX/PPTX）且 LlamaParse 已启用 →
 *       {@link LlamaParseDocumentParser}；云端失败或返回空结果时自动回退本地；</li>
 *   <li>XLSX → {@link ExcelDocumentParser}（本地，POI 转 Markdown 表格）；</li>
 *   <li>其余类型（TXT/MD 等）→ {@link TikaDocumentParser} 本地解析。</li>
 * </ul>
 */
@Component
@Primary
public class DocumentParserRouter implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserRouter.class);

    private final TikaDocumentParser tikaDocumentParser;
    private final ExcelDocumentParser excelDocumentParser;
    private final HtmlDocumentParser htmlDocumentParser;
    /** LlamaParse 未启用时 Bean 不存在，用 ObjectProvider 优雅处理 */
    private final ObjectProvider<LlamaParseDocumentParser> llamaParseProvider;

    public DocumentParserRouter(TikaDocumentParser tikaDocumentParser,
                                ExcelDocumentParser excelDocumentParser,
                                HtmlDocumentParser htmlDocumentParser,
                                ObjectProvider<LlamaParseDocumentParser> llamaParseProvider) {
        this.tikaDocumentParser = tikaDocumentParser;
        this.excelDocumentParser = excelDocumentParser;
        this.htmlDocumentParser = htmlDocumentParser;
        this.llamaParseProvider = llamaParseProvider;
    }

    @Override
    public List<Document> parse(MultipartFile file) {
        KbDocumentType type = KbDocumentType.fromFileName(file.getOriginalFilename());

        // HTML：始终本地解析
        if (type == KbDocumentType.HTML) {
            return htmlDocumentParser.parse(file);
        }

        // 云端解析（未启用时 Bean 不存在，直接走本地）
        LlamaParseDocumentParser llamaParse = llamaParseProvider.getIfAvailable();
        if (llamaParse != null && llamaParse.supports(type)) {
            try {
                List<Document> documents = llamaParse.parse(file);
                if (!documents.isEmpty()) {
                    return documents;
                }
                log.warn("LlamaParse 返回空结果，回退本地解析：{}", file.getOriginalFilename());
            } catch (Exception e) {
                log.warn("LlamaParse 解析失败，回退本地解析：{}：{}", file.getOriginalFilename(), e.getMessage());
            }
        }

        // 本地兜底（XLSX 走 POI 表格解析，其余走 Tika）
        if (type == KbDocumentType.XLSX) {
            return excelDocumentParser.parse(file);
        }
        return tikaDocumentParser.parse(file);
    }
}
