package cn.kong.kb.generation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 基于检索到的上下文，使用 LLM 生成答案。
 *
 * <p>同时支持同步生成（{@link #generate}）与流式生成
 * （{@link #streamGenerate}）。</p>
 */
@Component
public class AnswerGenerator {

    private static final Logger log = LoggerFactory.getLogger(AnswerGenerator.class);

    private final ChatModel chatModel;
    private final String promptTemplate;

    public AnswerGenerator(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.promptTemplate = loadPromptTemplate();
    }

    private String loadPromptTemplate() {
        try {
            return new String(
                    new ClassPathResource("prompts/answer-generation-prompt.txt")
                            .getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            // 与 prompts/answer-generation-prompt.txt 保持一致的中文兜底
            return """
                    你是一个专业的知识库助手。请严格基于以下提供的参考资料回答用户的问题。
                    如果参考资料中没有足够的信息来回答用户的问题，请明确说明"根据现有资料，无法找到确切答案"。
                    使用 [1]、[2] 等标记来引用具体的参考资料编号。

                    参考资料：
                    {context}

                    用户问题：{question}
                    """;
        }
    }

    /**
     * 将上下文与问题注入模板，构建最终的 prompt 字符串。
     */
    private String buildPrompt(String question, String context) {
        return promptTemplate
                .replace("{context}", context)
                .replace("{question}", question);
    }

    /**
     * 基于给定上下文与问题，同步生成答案。
     *
     * @param question 用户问题
     * @param context  已格式化的上下文字符串（带编号引用）
     * @return 生成的答案文本
     */
    public String generate(String question, String context) {
        String fullPrompt = buildPrompt(question, context);

        log.debug("生成答案，上下文长度：{} 字符", context.length());

        try {
            String answer = chatModel.call(new Prompt(fullPrompt))
                    .getResult().getOutput().getText();
            log.info("答案已生成：{} 字符", answer != null ? answer.length() : 0);
            return answer;
        } catch (Exception e) {
            log.error("答案生成失败", e);
            return "抱歉，生成答案时出现错误，请稍后重试。";
        }
    }

    /**
     * 流式生成答案，逐 token 返回。
     *
     * <p>每次发射的 String 是来自 LLM 的一个小文本片段，
     * 下游消费者负责拼接出完整答案。</p>
     *
     * @param question 用户问题
     * @param context  已格式化的上下文字符串（带编号引用）
     * @return 答案文本片段的 Flux；LLM 完成时结束
     */
    public Flux<String> streamGenerate(String question, String context) {
        String fullPrompt = buildPrompt(question, context);

        log.debug("流式生成答案，上下文长度：{} 字符", context.length());

        return chatModel.stream(new Prompt(fullPrompt))
                .map(resp -> {
                    try {
                        String text = resp.getResult().getOutput().getText();
                        return text == null ? "" : text;
                    } catch (Exception e) {
                        log.warn("从流式片段中提取文本失败", e);
                        return "";
                    }
                })
                .filter(text -> !text.isEmpty())
                .doOnComplete(() -> log.info("流式答案生成完成"))
                .doOnError(e -> log.error("流式答案生成失败", e));
    }
}
