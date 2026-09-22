package cn.kong.kb.api.dto;

import java.util.Map;

/**
 * 切片 DTO，用于接口响应（预览 / 查看 / 列表）。
 */
public record ChunkDto(
        String id,
        String content,
        String documentId,
        String documentName,
        int chunkIndex,
        String chunkType,
        String title,
        Map<String, Object> metadata
) {
    /**
     * 列表视图的精简构造：不含文档名与元数据。
     */
    public ChunkDto(String id, String content, String documentId,
                    int chunkIndex, String chunkType, String title) {
        this(id, content, documentId, null, chunkIndex, chunkType, title, null);
    }
}
