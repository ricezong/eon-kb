package cn.kong.kb.ingestion.parser;

import cn.kong.kb.domain.KbDocumentType;
import cn.kong.kb.domain.ParseMode;
import cn.kong.kb.ingestion.DocumentSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档解析路由器——摄入流水线实际注入的 {@code @Primary} 解析器。
 *
 * <p>按 {@link ParseMode} 与文档类型决定解析路径：</p>
 * <ul>
 *   <li>{@link ParseMode#LOCAL}：一律本地（HTML→jsoup，XLSX→POI，其余→Tika）；</li>
 *   <li>{@link ParseMode#AUTO}：白名单类型（{@code llamaparse.types}）且云端可用时走 LlamaParse，
 *       HTML 始终本地；云端失败/空结果回退本地；</li>
 *   <li>{@link ParseMode#CLOUD}：无视白名单，只要 LlamaParse 已装配就尝试云端；
 *       失败/空结果或未装配时回退本地。</li>
 * </ul>
 *
 * <p>本地兜底按类型分派：HTML → {@link HtmlDocumentParser}，XLSX → {@link ExcelDocumentParser}，
 * 其余 → {@link TikaDocumentParser}。</p>
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

    /** 云端解析器当前是否可用（Bean 已装配）。供上传选项接口探测。 */
    public boolean isCloudAvailable() {
        return llamaParseProvider.getIfAvailable() != null;
    }

    /** 默认按 {@link ParseMode#AUTO} 路由（{@link DocumentParser} 接口实现）。 */
    @Override
    public List<Document> parse(DocumentSource source) {
        return parse(source, ParseMode.AUTO);
    }

    /**
     * 按指定解析方式路由。
     *
     * @param source 文档输入快照
     * @param mode   解析方式（AUTO / LOCAL / CLOUD）
     */
    public List<Document> parse(DocumentSource source, ParseMode mode) {
        KbDocumentType type = KbDocumentType.fromFileName(source.filename());
        LlamaParseDocumentParser llamaParse = llamaParseProvider.getIfAvailable();

        if (shouldUseCloud(mode, type, llamaParse)) {
            try {
                List<Document> documents = llamaParse.parse(source);
                if (!documents.isEmpty()) {
                    return documents;
                }
                log.warn("LlamaParse 返回空结果，回退本地解析：{}", source.filename());
            } catch (Exception e) {
                log.warn("LlamaParse 解析失败，回退本地解析：{}", source.filename(), e);
            }
        }

        return parseLocal(source, type);
    }

    /**
     * 是否应尝试云端解析。
     * <ul>
     *   <li>LOCAL：永不；</li>
     *   <li>CLOUD：只要云端可用即尝试（无视类型白名单）；</li>
     *   <li>AUTO：云端可用 + 类型在白名单内 + 非 HTML（HTML 本地 jsoup 质量更佳）。</li>
     * </ul>
     */
    private boolean shouldUseCloud(ParseMode mode, KbDocumentType type, LlamaParseDocumentParser llamaParse) {
        if (llamaParse == null || mode == ParseMode.LOCAL) {
            return false;
        }
        if (mode == ParseMode.CLOUD) {
            return true;
        }
        // AUTO
        return type != KbDocumentType.HTML && llamaParse.supports(type);
    }

    /** 本地解析：按类型分派到对应解析器。 */
    private List<Document> parseLocal(DocumentSource source, KbDocumentType type) {
        if (type == KbDocumentType.HTML) {
            return htmlDocumentParser.parse(source);
        }
        if (type == KbDocumentType.XLSX) {
            return excelDocumentParser.parse(source);
        }
        return tikaDocumentParser.parse(source);
    }
}
