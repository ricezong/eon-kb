package cn.kong.kb.ingestion.chunker;

import cn.kong.kb.domain.KbChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 纯文本与 Markdown 文件的分块器。
 * 按空行（段落）切分，合并过小段落、拆分过大段落。
 */
@Component
public class ParagraphChunker implements ChunkingStrategy {

    private static final Logger log = LoggerFactory.getLogger(ParagraphChunker.class);

    @Value("${app.chunking.target-tokens:512}")
    private int targetTokens;

    @Value("${app.chunking.max-tokens:1024}")
    private int maxTokens;

    @Value("${app.chunking.overlap-tokens:50}")
    private int overlapTokens;

    @Override
    public List<KbChunk> chunk(List<Document> documents, UUID documentId) {
        List<KbChunk> kbChunks = new ArrayList<>();
        int chunkIndex = 0;

        for (Document doc : documents) {
            String content = doc.getText();
            if (content == null || content.isBlank()) continue;

            // 按空行切分
            String[] paragraphs = content.split("\n\n+");
            StringBuilder currentChunk = new StringBuilder();
            String chunkTitle = extractFirstHeading(content);

            for (String paragraph : paragraphs) {
                if (paragraph.isBlank()) continue;

                // 检查是否为标题
                String heading = extractHeading(paragraph);
                if (heading != null) {
                    if (!currentChunk.isEmpty()) {
                        kbChunks.add(createChunk(currentChunk.toString().trim(), documentId,
                                chunkIndex++, "paragraph", chunkTitle, doc));
                        currentChunk = new StringBuilder();
                    }
                    chunkTitle = heading;
                }

                int currentTokens = ChunkTextUtils.estimateTokens(currentChunk.toString());
                int paragraphTokens = ChunkTextUtils.estimateTokens(paragraph);

                if (currentTokens + paragraphTokens > maxTokens && !currentChunk.isEmpty()) {
                    kbChunks.add(createChunk(currentChunk.toString().trim(), documentId,
                            chunkIndex++, "paragraph", chunkTitle, doc));
                    currentChunk = new StringBuilder();
                }

                currentChunk.append(paragraph).append("\n\n");

                // 每次追加后检查是否需要按 token 上限强制拆分
                if (ChunkTextUtils.estimateTokens(currentChunk.toString()) > maxTokens) {
                    // 按单行拆分，按 targetTokens 窗口重组
                    String[] lines = currentChunk.toString().split("\n");
                    StringBuilder sub = new StringBuilder();
                    for (String line : lines) {
                        int subTokens = ChunkTextUtils.estimateTokens(sub.toString());
                        int lineTokens = ChunkTextUtils.estimateTokens(line);
                        if (subTokens + lineTokens > targetTokens && !sub.isEmpty()) {
                            kbChunks.add(createChunk(sub.toString().trim(), documentId,
                                    chunkIndex++, "paragraph", chunkTitle, doc));
                            // 重叠：取最后 overlapTokens 的内容
                            String overlap = ChunkTextUtils.buildOverlapText(sub.toString(), overlapTokens);
                            sub = new StringBuilder(overlap);
                        }
                        sub.append(line).append("\n");
                    }
                    currentChunk = sub;
                }
            }

            if (!currentChunk.isEmpty()) {
                kbChunks.add(createChunk(currentChunk.toString().trim(), documentId,
                        chunkIndex++, "paragraph", chunkTitle, doc));
            }
        }

        log.info("ParagraphChunker 为文档 {} 产出 {} 个切片", documentId, kbChunks.size());
        return kbChunks;
    }

    private String extractFirstHeading(String content) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            String heading = extractHeading(line.trim());
            if (heading != null) return heading;
        }
        return "";
    }

    private String extractHeading(String text) {
        if (text.startsWith("#")) {
            return text.replaceAll("^#+\\s*", "").trim();
        }
        return null;
    }

    private KbChunk createChunk(String content, UUID documentId, int chunkIndex,
                                String chunkType, String title, Document doc) {
        KbChunk kbChunk = new KbChunk(content, documentId, chunkIndex, chunkType, title);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_id", documentId.toString());
        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_type", chunkType);
        metadata.put("title", title != null ? title : "");
        metadata.putAll(doc.getMetadata());
        kbChunk.setMetadata(metadata);
        return kbChunk;
    }
}
