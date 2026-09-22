package cn.kong.kb.tool;

/**
 * 单条检索到的知识片段（工具返回给 agent 的最小单元）。
 *
 * @param chunkId      切片 ID
 * @param documentId   所属文档 ID
 * @param documentName 所属文档名（用于 agent 侧生成引用来源）
 * @param title        切片标题（可能为 null）
 * @param chunkType    切片类型（heading / slide / sheet / paragraph 等）
 * @param content      切片正文原文
 * @param score        相关性分数（重排后为 rerank 分数）
 */
public record RetrievedChunk(
        String chunkId,
        String documentId,
        String documentName,
        String title,
        String chunkType,
        String content,
        double score
) {}
