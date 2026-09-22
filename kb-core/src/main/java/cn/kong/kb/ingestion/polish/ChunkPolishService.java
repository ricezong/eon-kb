package cn.kong.kb.ingestion.polish;

import cn.kong.kb.domain.KbChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 切片润色服务：使用 LLM 对分块内容进行润色，提升向量化质量。
 * <p>
 * 润色后的文本直接替换 {@link KbChunk#setContent(String)} 原文，
 * 后续向量化和数据库存储均使用润色后的内容。
 * </p>
 */
@Component
public class ChunkPolishService {

    private static final Logger log = LoggerFactory.getLogger(ChunkPolishService.class);

    private final ChatModel chatModel;
    private final String promptTemplate;
    private final RateLimiter rateLimiter;

    @Value("${app.polish.enabled:true}")
    private boolean enabled;

    @Value("${app.polish.max-content-length:8000}")
    private int maxContentLength;

    public ChunkPolishService(ChatModel chatModel,
                               @Value("${app.polish.max-requests-per-minute:30}") int maxRequestsPerMinute) {
        this.chatModel = chatModel;
        this.promptTemplate = loadPromptTemplate();
        this.rateLimiter = new RateLimiter(maxRequestsPerMinute);
    }

    /**
     * 对一批切片进行润色，直接修改 {@code KbChunk.content}。
     * <p>
     * 润色失败的切片保留原文，记录 WARN 日志，不抛出异常。
     * </p>
     *
     * @param chunks 待润色的切片列表
     */
    public void polishChunks(List<KbChunk> chunks) {
        if (!enabled) {
            log.debug("润色功能已禁用，跳过");
            return;
        }

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (KbChunk chunk : chunks) {
            String original = chunk.getContent();
            if (original == null || original.isBlank()) {
                skipCount++;
                continue;
            }

            // 超长内容跳过润色
            if (original.length() > maxContentLength) {
                log.warn("切片内容超长({}字符)，跳过润色", original.length());
                skipCount++;
                continue;
            }

            try {
                rateLimiter.acquire();
                String polished = callAi(original);
                if (polished != null && !polished.isBlank()) {
                    chunk.setContent(polished.trim());
                    successCount++;
                    log.debug("切片润色成功：{}字符 → {}字符", original.length(), polished.length());
                } else {
                    log.warn("AI 返回空内容，保留原文");
                    failCount++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("润色等待被中断，保留原文");
                failCount++;
                break; // 中断后不再继续处理剩余切片
            } catch (Exception e) {
                log.warn("切片润色失败，保留原文: {}", e);
                failCount++;
            }
        }

        log.info("润色完成：成功 {}，跳过 {}，失败 {}", successCount, skipCount, failCount);
    }

    private String callAi(String content) {
        String prompt = promptTemplate.replace("{content}", content);
        return chatModel.call(new Prompt(prompt))
                .getResult().getOutput().getText();
    }

    private String loadPromptTemplate() {
        try {
            return new String(
                    new ClassPathResource("prompts/chunk-polish-prompt.txt")
                            .getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("加载润色提示词模板失败，使用默认模板", e);
            return """
                    请润色以下文本内容，清理格式噪声，保持原意不变。不要添加任何解释。
                    
                    {content}
                    """;
        }
    }
}
