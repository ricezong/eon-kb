package cn.kong.kb.domain;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 切片实体，表示从文档中切分出的一段文本。
 *
 * <p>主键在构造时即生成：向量入库（PgVectorStore 的 Document ID）、
 * 业务列写入（kb_chunks 行）与检索回读使用同一个 ID，
 * 摄入侧因此无需"先插入再回写主键"。</p>
 */
public class KbChunk {

    private UUID id;
    private String content;
    private Map<String, Object> metadata;
    private UUID documentId;
    private int chunkIndex;
    private String chunkType;
    private String title;
    private LocalDateTime createdAt;

    // 检索结果中使用的临时字段
    private double score;

    public KbChunk() {}

    public KbChunk(String content, UUID documentId, int chunkIndex, String chunkType, String title) {
        this.id = UUID.randomUUID();
        this.content = content;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.chunkType = chunkType;
        this.title = title;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getChunkType() { return chunkType; }
    public void setChunkType(String chunkType) { this.chunkType = chunkType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
