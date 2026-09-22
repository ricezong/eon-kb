package cn.kong.kb.ingestion.chunker;

import cn.kong.kb.domain.KbChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * PowerPoint 演示文稿分块器。
 * 每张幻灯片作为一个切片，包含标题 + 正文 + 备注。
 */
@Component
public class SlideChunker implements ChunkingStrategy {

    private static final Logger log = LoggerFactory.getLogger(SlideChunker.class);

    @Override
    public List<KbChunk> chunk(List<Document> documents, UUID documentId) {
        List<KbChunk> kbChunks = new ArrayList<>();
        int chunkIndex = 0;

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String content = doc.getText();
            if (content == null || content.isBlank()) continue;

            String title = extractSlideTitle(content, doc);

            KbChunk kbChunk = new KbChunk(content.trim(), documentId, chunkIndex, "slide", title);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", documentId.toString());
            metadata.put("chunk_index", chunkIndex);
            metadata.put("chunk_type", "slide");
            metadata.put("slide_number", i + 1);
            metadata.put("title", title);
            metadata.putAll(doc.getMetadata());
            kbChunk.setMetadata(metadata);

            kbChunks.add(kbChunk);
            chunkIndex++;
        }

        log.info("SlideChunker 为文档 {} 产出 {} 个切片", documentId, kbChunks.size());
        return kbChunks;
    }

    private String extractSlideTitle(String content, Document doc) {
        // 优先从元数据中获取标题
        Object titleMeta = doc.getMetadata().get("slide_title");
        if (titleMeta != null && !titleMeta.toString().isBlank()) {
            return titleMeta.toString();
        }

        // 回退：取第一个非空行
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
            }
        }
        return "Slide";
    }
}
