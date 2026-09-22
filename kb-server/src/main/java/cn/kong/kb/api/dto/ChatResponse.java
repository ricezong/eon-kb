package cn.kong.kb.api.dto;

import java.util.List;

/**
 * 聊天响应 DTO，包含生成的答案与引用来源。
 */
public record ChatResponse(
        String answer,
        List<CitationDto> citations
) {}
