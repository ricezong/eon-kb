package cn.kong.kb.tool;

import java.util.List;

/**
 * 知识库检索工具的返回结果。
 *
 * <p>工具为 <b>retrieval-only</b>：只返回带来源的原文片段，不生成答案，
 * 由 agent 自身的 LLM 组织回答。</p>
 *
 * @param query  实际用于检索的查询
 * @param total  返回片段数
 * @param chunks 检索片段列表，按相关性降序
 */
public record RetrievalResult(
        String query,
        int total,
        List<RetrievedChunk> chunks
) {
    /** 空结果快捷构造。 */
    public static RetrievalResult empty(String query) {
        return new RetrievalResult(query, 0, List.of());
    }
}
