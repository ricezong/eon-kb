package cn.kong.kb.ingestion.parser;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTML 文档解析器：jsoup 负责编码探测与安全清洗，
 * flexmark 负责转换为 Markdown，之后与 MD 文档同路分块（ParagraphChunker）。
 */
@Component
public class HtmlDocumentParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(HtmlDocumentParser.class);

    private final HtmlToMarkdownConverter converter;

    public HtmlDocumentParser(HtmlToMarkdownConverter converter) {
        this.converter = converter;
    }

    @Override
    public List<Document> parse(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            // charsetName 传 null：jsoup 依据 BOM / meta charset 自动探测编码
            var dom = Jsoup.parse(file.getInputStream(), null, "");
            String title = dom.title();

            String markdown = converter.convert(dom);
            if (markdown.isBlank()) {
                log.warn("HTML 文档转换为空 Markdown：{}", fileName);
                return List.of();
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source_file", fileName);
            metadata.put("file_type", "HTML");
            if (title != null && !title.isBlank()) {
                metadata.put("title", title.strip());
            }
            return List.of(new Document(markdown, metadata));
        } catch (Exception e) {
            throw new IllegalStateException("HTML 解析失败：" + file.getOriginalFilename(), e);
        }
    }
}
