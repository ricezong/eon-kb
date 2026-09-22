package cn.kong.kb.api.dto;

/**
 * 创建知识库请求 DTO。
 */
public record CreateKnowledgeBaseRequest(
        String name,
        String description,
        String color
) {}
