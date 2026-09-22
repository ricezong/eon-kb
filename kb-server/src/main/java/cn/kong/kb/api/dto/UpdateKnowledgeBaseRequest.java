package cn.kong.kb.api.dto;

/**
 * 更新知识库请求 DTO。
 *
 * <p>各字段可选：为 {@code null} 表示保持原值不变。</p>
 */
public record UpdateKnowledgeBaseRequest(
        String name,
        String description,
        String color
) {}
