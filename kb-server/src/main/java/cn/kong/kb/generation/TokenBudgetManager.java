package cn.kong.kb.generation;

import cn.kong.kb.domain.KbChunk;
import cn.kong.kb.ingestion.chunker.ChunkTextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下文 token 预算管理器。
 * 估算 token 数量，并对切片进行截断以适配预算。
 */
@Component
public class TokenBudgetManager {

    private static final Logger log = LoggerFactory.getLogger(TokenBudgetManager.class);

    /** 预算兜底下限：防止配置不合理导致上下文被压为零 */
    private static final int MIN_CONTEXT_BUDGET = 100;

    @Value("${app.generation.max-context-tokens:6000}")
    private int maxContextTokens;

    @Value("${app.generation.system-prompt-tokens:500}")
    private int systemPromptTokens;

    @Value("${app.generation.question-prompt-tokens:200}")
    private int questionPromptTokens;

    @Value("${app.generation.output-reserve-tokens:1000}")
    private int outputReserveTokens;

    /**
     * 计算上下文切片可用的 token 预算：
     * 模型总窗口 − 系统提示词 − 用户问题 − 答案输出预留。
     *
     * <p>配置不合理（差值低于 {@link #MIN_CONTEXT_BUDGET}）时记录告警，
     * 并回退为 max-context-tokens，保证生成流程仍可用。</p>
     */
    public int getAvailableContextBudget() {
        int budget = maxContextTokens - systemPromptTokens - questionPromptTokens - outputReserveTokens;
        if (budget < MIN_CONTEXT_BUDGET) {
            log.warn("app.generation 配置不合理：{} - {} - {} - {} = {}，低于下限 {}，回退为 max-context-tokens={}",
                    maxContextTokens, systemPromptTokens, questionPromptTokens, outputReserveTokens,
                    budget, MIN_CONTEXT_BUDGET, maxContextTokens);
            return maxContextTokens;
        }
        return budget;
    }

    /**
     * 按预算过滤并截断切片，使其总量不超过预算。
     * 高分切片优先保留。
     *
     * @param kbChunks 已按分数降序排列的切片
     * @return 截断后适配预算的切片条目列表
     */
    public List<ChunkBudgetEntry> fitToBudget(List<KbChunk> kbChunks) {
        int budget = getAvailableContextBudget();
        List<ChunkBudgetEntry> entries = new ArrayList<>();
        int usedTokens = 0;

        for (KbChunk kbChunk : kbChunks) {
            int chunkTokens = ChunkTextUtils.estimateTokens(kbChunk.getContent());

            if (usedTokens + chunkTokens > budget) {
                // 尝试截断该切片
                int remaining = budget - usedTokens;
                if (remaining > MIN_CONTEXT_BUDGET) { // 仅当剩余空间足以容纳有意义内容时才纳入
                    String truncated = truncateToTokens(kbChunk.getContent(), remaining);
                    entries.add(new ChunkBudgetEntry(kbChunk, truncated, remaining));
                    usedTokens += remaining;
                }
                break; // 预算已用尽
            }

            entries.add(new ChunkBudgetEntry(kbChunk, kbChunk.getContent(), chunkTokens));
            usedTokens += chunkTokens;
        }

        return entries;
    }

    /**
     * 将文本截断至大约指定的 token 数。
     */
    private String truncateToTokens(String text, int maxTokens) {
        // 估算：中文约 1 token ≈ 0.67 字；英文约 1 token ≈ 4 字
        // 混合内容保守取 1.5 字 / token
        int maxChars = (int) (maxTokens * 1.5);
        if (text.length() <= maxChars) return text;

        String truncated = text.substring(0, maxChars);
        // 尽量在句子边界处断开
        int lastPeriod = Math.max(
                truncated.lastIndexOf('。'),
                Math.max(truncated.lastIndexOf('.'),
                        Math.max(truncated.lastIndexOf('！'), truncated.lastIndexOf('？')))
        );
        if (lastPeriod > maxChars / 2) {
            truncated = truncated.substring(0, lastPeriod + 1);
        }
        return truncated + "...";
    }

    /**
     * 切片预算条目，持有切片及其按预算调整后的内容。
     */
    public record ChunkBudgetEntry(
            KbChunk kbChunk,
            String adjustedContent,
            int estimatedTokens
    ) {}
}
