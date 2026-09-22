package cn.kong.kb.tool;

import java.util.List;

/**
 * 知识库检索工具门面 —— kb-core 对外暴露的唯一入口。
 *
 * <p>agent 通过一次方法调用即可完成"混合检索 → 重排"，拿到带来源的原文片段，
 * 再交由自身 LLM 组织答案。该接口与具体 agent 框架无关；默认实现
 * {@code DefaultKnowledgeBaseTool} 另用 Spring AI 的 {@code @Tool} 注解标注，
 * 可直接注册为 function-calling 工具。</p>
 */
public interface KnowledgeBaseTool {

    /**
     * 在知识库中检索与查询相关的片段。
     *
     * @param query            用户问题或检索关键词，不可为空
     * @param knowledgeBaseIds 限定的知识库 ID（字符串形式）；为 null 或空表示全局检索
     * @param topK             返回片段数上限；为 null 或非正数时使用默认值
     * @return 检索结果（按相关性降序的片段 + 来源）
     */
    RetrievalResult search(String query, List<String> knowledgeBaseIds, Integer topK);
}
