package cn.kong.kb.ingestion.parser;

import cn.kong.kb.domain.KbDocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 使用 Apache Tika 的本地文档解析器（PDF、Word、PPT、TXT、MD）。
 *
 * <p>Excel 由 {@link ExcelDocumentParser} 单独处理（POI 保留表格结构），
 * HTML 由 {@link HtmlDocumentParser} 处理。类型路由见 {@link DocumentParserRouter}。</p>
 */
@Component
public class TikaDocumentParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(TikaDocumentParser.class);

    @Override
    public List<Document> parse(MultipartFile file) {
        KbDocumentType type = KbDocumentType.fromFileName(file.getOriginalFilename());

        try {
            return parseWithTika(file, type);
        } catch (Exception e) {
            log.error("解析文档失败：{}", file.getOriginalFilename(), e);
            throw new RuntimeException("文档解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 使用 Apache Tika 解析文档（PDF、Word、PPT、TXT、MD）。
     */
    private List<Document> parseWithTika(MultipartFile file, KbDocumentType type) {
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.get();

            // 丰富元数据
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                metadata.put("source_file", file.getOriginalFilename());
                metadata.put("file_type", type.name());
                metadata.put("page_number", i + 1);
                doc.getMetadata().putAll(metadata);
            }

            // PPT 后处理：清理备注与幻灯片标题
            if (type == KbDocumentType.PPTX) {
                documents = cleanPptContent(documents);
            }

            log.info("使用 Tika 解析 {}：得到 {} 个文档片段",
                    file.getOriginalFilename(), documents.size());
            return documents;
        } catch (Exception e) {
            throw new RuntimeException("Tika 解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 清理 PPT 内容：从 Tika 输出中提取幻灯片标题与备注。
     */
    private List<Document> cleanPptContent(List<Document> documents) {
        List<Document> cleaned = new ArrayList<>();
        for (Document doc : documents) {
            String content = doc.getText();
            if (content == null || content.isBlank()) continue;

            Map<String, Object> metadata = new HashMap<>(doc.getMetadata());

            // 尝试从第一行提取幻灯片标题
            String[] lines = content.split("\n", 2);
            if (lines.length > 0 && !lines[0].isBlank()) {
                metadata.put("slide_title", lines[0].trim());
            }

            Document cleanedDoc = new Document(content, metadata);
            cleaned.add(cleanedDoc);
        }
        return cleaned;
    }
}
