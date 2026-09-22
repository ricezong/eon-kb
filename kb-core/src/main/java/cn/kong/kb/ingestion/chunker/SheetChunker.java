package cn.kong.kb.ingestion.chunker;

import cn.kong.kb.domain.KbChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Excel 文件分块器。
 * 解析阶段已将每个工作表转为 Markdown 表格，此处对大表按行窗口进一步切分。
 * 对单个切片进行 token 估算，确保不超过 Embedding 模型的最大 token 限制。
 */
@Component
public class SheetChunker implements ChunkingStrategy {

    private static final Logger log = LoggerFactory.getLogger(SheetChunker.class);
    private static final int MAX_ROWS_PER_CHUNK = 10;
    /** 单个切片最大字符数（保守值，确保不超出 Embedding 模型限制） */
    private static final int MAX_CHUNK_CHARS = 1000;
    /** 最小行数，防止拆分后切片过小 */
    private static final int MIN_ROWS_PER_CHUNK = 1;

    @Override
    public List<KbChunk> chunk(List<Document> documents, UUID documentId) {
        List<KbChunk> kbChunks = new ArrayList<>();
        int chunkIndex = 0;

        for (Document doc : documents) {
            String content = doc.getText();
            if (content == null || content.isBlank()) continue;

            String sheetName = String.valueOf(doc.getMetadata().getOrDefault("sheet_name", "Sheet"));

            String[] lines = content.split("\n");

            if (lines.length <= MAX_ROWS_PER_CHUNK + 2) {
                // 足够小，可作为单个切片（表头 + 分隔行 + 数据行）
                String trimmed = content.trim();
                if (trimmed.length() <= MAX_CHUNK_CHARS) {
                    KbChunk kbChunk = new KbChunk(trimmed, documentId, chunkIndex, "sheet", sheetName);
                    kbChunk.setMetadata(buildMetadata(documentId, chunkIndex, "sheet", sheetName, doc));
                    kbChunks.add(kbChunk);
                    chunkIndex++;
                } else {
                    // 小表但 token/字符 超限（行内容过长），按行拆分
                    chunkIndex = splitLargeTable(lines, sheetName, documentId, chunkIndex, kbChunks, doc);
                }
            } else {
                // 大表：按行窗口切分，每个切片保留表头
                String header = lines[0]; // 表头
                String separator = lines[1]; // |---|---|

                int start = 2;
                while (start < lines.length) {
                    int end = Math.min(start + MAX_ROWS_PER_CHUNK, lines.length);

                    // 逐步缩小窗口直到 token 不超限
                    StringBuilder sb;
                    while (true) {
                        sb = new StringBuilder();
                        sb.append(header).append("\n");
                        sb.append(separator).append("\n");
                        for (int r = start; r < end; r++) {
                            sb.append(lines[r]).append("\n");
                        }
                        if (sb.length() <= MAX_CHUNK_CHARS
                                || end - start <= MIN_ROWS_PER_CHUNK) {
                            break;
                        }
                        // 减少行数重试
                        end = Math.max(start + MIN_ROWS_PER_CHUNK, end - (end - start) / 2);
                    }

                    String chunkTitle = sheetName + "（第 " + (start - 1) + "-" + (end - 1) + " 行）";
                    KbChunk kbChunk = new KbChunk(sb.toString().trim(), documentId, chunkIndex, "sheet", chunkTitle);
                    kbChunk.setMetadata(buildMetadata(documentId, chunkIndex, "sheet", chunkTitle, doc));
                    kbChunks.add(kbChunk);
                    chunkIndex++;
                    start = end;
                }
            }
        }

        log.info("SheetChunker 为文档 {} 产出 {} 个切片", documentId, kbChunks.size());
        return kbChunks;
    }

    /**
     * 将超大表格（行数不多但 token 超限）按行拆分为多个切片。
     */
    private int splitLargeTable(String[] lines, String sheetName, UUID documentId,
                                int chunkIndex, List<KbChunk> kbChunks, Document doc) {
        String header = lines.length > 0 ? lines[0] : "";
        String separator = lines.length > 1 ? lines[1] : "";
        int dataStart = lines.length > 2 ? 2 : lines.length;

        int start = dataStart;
        while (start < lines.length) {
            int end = start + 1;
            StringBuilder sb = new StringBuilder();
            sb.append(header).append("\n").append(separator).append("\n");
            sb.append(lines[start]).append("\n");

            while (end < lines.length) {
                String trial = sb.toString() + lines[end] + "\n";
                if (trial.length() > MAX_CHUNK_CHARS
                        && end - start >= MIN_ROWS_PER_CHUNK) {
                    break;
                }
                sb.append(lines[end]).append("\n");
                end++;
            }

            String chunkTitle = sheetName + "（第 " + (start - 1) + "-" + (end - 1) + " 行）";
            KbChunk kbChunk = new KbChunk(sb.toString().trim(), documentId, chunkIndex, "sheet", chunkTitle);
            kbChunk.setMetadata(buildMetadata(documentId, chunkIndex, "sheet", chunkTitle, doc));
            kbChunks.add(kbChunk);
            chunkIndex++;
            start = end;
        }
        return chunkIndex;
    }

    private Map<String, Object> buildMetadata(UUID documentId, int chunkIndex,
                                               String chunkType, String title, Document doc) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_id", documentId.toString());
        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_type", chunkType);
        metadata.put("title", title);
        metadata.putAll(doc.getMetadata());
        return metadata;
    }
}
