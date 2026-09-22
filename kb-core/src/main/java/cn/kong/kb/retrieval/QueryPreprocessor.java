package cn.kong.kb.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 查询预处理器，使用 HyDE（假设性文档嵌入）或查询改写来提升检索质量。
 */
@Component
public class QueryPreprocessor {

    private static final Logger log = LoggerFactory.getLogger(QueryPreprocessor.class);

    /** 短问题阈值：低于此字符数的问题跳过 LLM 改写，直接使用原始查询 */
    private static final int SHORT_QUERY_THRESHOLD = 15;

    private final ChatModel chatModel;

    @Value("${app.retrieval.hyde-enabled:false}")
    private boolean hydeEnabled;

    private String hydePromptTemplate;
    private String rewritePromptTemplate;

    public QueryPreprocessor(ChatModel chatModel) {
        this.chatModel = chatModel;
        loadPrompts();
    }

    private void loadPrompts() {
        try {
            hydePromptTemplate = new String(
                    new ClassPathResource("prompts/hyde-prompt.txt").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            hydePromptTemplate = "Please provide a detailed factual answer to the following question. " +
                    "Write it as if you were writing a reference document paragraph:\n\n{query}";
        }
        try {
            rewritePromptTemplate = new String(
                    new ClassPathResource("prompts/query-rewrite-prompt.txt").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            rewritePromptTemplate = "Rewrite the following question into a clear, precise statement " +
                    "suitable for document retrieval. Output only the rewritten query:\n\n{query}";
        }
    }

    /**
     * 预处理查询。若启用 HyDE，则生成假设性答案；否则对查询进行改写以提升检索效果。
     *
     * @param originalQuery 用户原始查询
     * @return 用于向量嵌入 / 检索的预处理后查询文本
     */
    public String preprocess(String originalQuery) {
        // 短问题已经是清晰的查询意图，无需 LLM 改写，节省延迟和费用
        if (originalQuery.trim().length() <= SHORT_QUERY_THRESHOLD) {
            log.debug("短问题（≤{}字符），跳过改写：{}", SHORT_QUERY_THRESHOLD, originalQuery);
            return originalQuery;
        }
        if (!hydeEnabled) {
            // 默认：使用查询改写提升检索效果
            return rewriteQuery(originalQuery);
        }
        return generateHyde(originalQuery);
    }

    /**
     * 生成假设性文档嵌入（HyDE）。
     * 借助 LLM 生成一段假设性答案，以弥合语义鸿沟。
     */
    private String generateHyde(String query) {
        try {
            String prompt = hydePromptTemplate.replace("{query}", query);
            String hydeText = chatModel.call(new Prompt(prompt))
                    .getResult().getOutput().getText();
            log.debug("为查询生成 HyDE：{} -> {}", query, hydeText.substring(0, Math.min(100, hydeText.length())));
            return hydeText;
        } catch (Exception e) {
            log.warn("HyDE 生成失败，回退为原始查询", e);
            return query;
        }
    }

    /**
     * 改写查询，使其更精确、更利于检索。
     * 将口语化的问题转换为陈述性表述。
     */
    private String rewriteQuery(String query) {
        try {
            String prompt = rewritePromptTemplate.replace("{query}", query);
            String rewritten = chatModel.call(new Prompt(prompt))
                    .getResult().getOutput().getText();
            log.debug("查询改写：{} -> {}", query, rewritten);
            return rewritten.trim();
        } catch (Exception e) {
            log.warn("查询改写失败，使用原始查询", e);
            return query;
        }
    }
}
