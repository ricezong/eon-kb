package cn.kong.kb.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档 DTO，用于接口响应。
 */
public record DocumentDto(
        String id,
        String fileName,
        String fileType,
        Long fileSize,
        int chunkCount,
        String status,
        String parseMode,
        String errorMessage,
        LocalDateTime createdAt,
        List<KnowledgeBaseDto> knowledgeBases  // 文档归属的知识库列表
) {}
