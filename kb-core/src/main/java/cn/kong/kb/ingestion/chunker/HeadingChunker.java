package cn.kong.kb.ingestion.chunker;

import cn.kong.kb.domain.KbChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * PDF / Word 文档分块器。
 * 按标题 / 段落边界切分，保持语义完整性。
 * 对过大的章节回退使用基于 token 的滑动窗口。
 */
@Component
public class HeadingChunker implements ChunkingStrategy {

    private static final Logger log = LoggerFactory.getLogger(HeadingChunker.class);

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

        // 将所有文档片段合并为整段文本，再进行智能切分
        StringBuilder fullText = new StringBuilder();
        String currentTitle = "";
        // 透传解析器产出的元数据（如 LlamaParse 的页级信息）。
        // 本策略将全部片段合并后再切分，无法按页关联，
        // 因此取首个非空元数据集作为文档级元数据兜底。
        Map<String, Object> sourceMetadata = null;

        for (Document doc : documents) {
            String text = doc.getText();
            if (text == null || text.isBlank()) continue;

            if (sourceMetadata == null && doc.getMetadata() != null && !doc.getMetadata().isEmpty()) {
                sourceMetadata = doc.getMetadata();
            }

            // 检查该片段是否包含标题
            String title = extractTitle(text);
            if (title != null) {
                currentTitle = title;
            }

            fullText.append(text).append("\n\n");
        }

        if (fullText.isEmpty()) return kbChunks;

        // 按段落切分
        String[] paragraphs = fullText.toString().split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();
        String chunkTitle = "";

        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) continue;

            String title = extractTitle(paragraph);
            if (title != null) {
                // 在进入新章节前，先保存当前切片
                if (!currentChunk.isEmpty()) {
                    kbChunks.add(createChunk(currentChunk.toString().trim(), documentId,
                            chunkIndex++, "heading", chunkTitle, sourceMetadata));
                    currentChunk = new StringBuilder();
                }
                chunkTitle = title;
            }

            int currentTokenEstimate = ChunkTextUtils.estimateTokens(currentChunk.toString());
            int paragraphTokens = ChunkTextUtils.estimateTokens(paragraph);

            if (currentTokenEstimate + paragraphTokens > maxTokens && !currentChunk.isEmpty()) {
                // 当前切片已满，先保存
                kbChunks.add(createChunk(currentChunk.toString().trim(), documentId,
                        chunkIndex++, "heading", chunkTitle, sourceMetadata));

                // 从上一个切片中补充重叠内容
                String overlap = ChunkTextUtils.buildOverlapText(currentChunk.toString(), overlapTokens);
                currentChunk = new StringBuilder(overlap);
            }

            currentChunk.append(paragraph).append("\n\n");
        }

        // 保存最后一个切片
        if (!currentChunk.isEmpty()) {
            kbChunks.add(createChunk(currentChunk.toString().trim(), documentId,
                    chunkIndex++, "heading", chunkTitle, sourceMetadata));
        }

        log.info("HeadingChunker 为文档 {} 产出 {} 个切片", documentId, kbChunks.size());
        return kbChunks;
    }

    private String extractTitle(String text) {
        if (text == null) return null;
        String firstLine = text.split("\n")[0].trim();
        // 识别标题模式（Markdown 标题或常见文档标题）
        if (firstLine.startsWith("#") || firstLine.matches("^(第[一二三四五六七八九十]+[章节篇部])\\s*.*")) {
            return firstLine.replaceAll("^#+\\s*", "").trim();
        }
        return null;
    }

    private KbChunk createChunk(String content, UUID documentId, int chunkIndex,
                                String chunkType, String title, Map<String, Object> sourceMetadata) {
        KbChunk kbChunk = new KbChunk(content, documentId, chunkIndex, chunkType, title);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_id", documentId.toString());
        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_type", chunkType);
        metadata.put("title", title != null ? title : "");
        if (sourceMetadata != null) {
            metadata.putAll(sourceMetadata);
        }
        kbChunk.setMetadata(metadata);
        return kbChunk;
    }
}
