package cn.kong.kb.api.dto;

import java.time.LocalDateTime;

/**
 * 知识库分类 DTO，用于接口响应。
 */
public record KnowledgeBaseDto(
        String id,
        String name,
        String description,
        String color,
        int documentCount,
        LocalDateTime createdAt
) {}
