package cn.kong.kb.api.dto;

/**
 * 引用来源 DTO，用于切片预览展示。
 */
public record CitationDto(
        String id,
        int index,
        String documentName,
        String chunkType,
        String title,
        String preview,
        double score
) {}
