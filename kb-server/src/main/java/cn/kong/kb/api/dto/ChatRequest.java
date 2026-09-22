package cn.kong.kb.api.dto;

import java.util.List;

/**
 * 聊天请求 DTO。
 */
public record ChatRequest(
        String question,
        List<String> knowledgeBaseIds   // 可选：指定知识库 ID 列表，null 或空表示全局搜索
) {}
