package cn.kong.kb.api.dto;

import java.util.List;

/**
 * 文档-知识库关联请求 DTO。
 */
public record AssignKnowledgeBasesRequest(List<String> knowledgeBaseIds) {}
