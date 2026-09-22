package cn.kong.kb.chat;

import cn.kong.kb.api.dto.ChatResponse;
import cn.kong.kb.api.dto.CitationDto;
import cn.kong.kb.domain.KbChunk;
import cn.kong.kb.generation.AnswerGenerator;
import cn.kong.kb.generation.CitationFormatter;
import cn.kong.kb.generation.TokenBudgetManager;
import cn.kong.kb.rerank.RerankService;
import cn.kong.kb.retrieval.HybridSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 端到端聊天服务，编排如下流程：
 * 混合检索 → 重排 → Token 预算 → 答案生成 → 引用组装
 *
 * <p>同步（{@link #chat}）与流式（{@link #streamChat}）两条通道
 * 复用同一条检索管线 {@link #prepareContext}，仅在答案生成环节分叉，
 * 避免两份复制的流水线代码各自漂移。</p>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** SSE 连接超时时间：3 分钟。 */
    private static final long SSE_TIMEOUT_MS = 180_000L;

    /** 无检索结果时的固定答复 */
    private static final String NO_RESULT_ANSWER =
            "抱歉，我在知识库中没有找到与您问题相关的内容。请尝试换一种方式提问，或确认相关文档是否已上传。";

    private final HybridSearchService hybridSearchService;
    private final RerankService rerankService;
    private final TokenBudgetManager tokenBudgetManager;
    private final AnswerGenerator answerGenerator;
    private final CitationFormatter citationFormatter;
    private final Executor searchExecutor;

    @Value("${app.rerank.top-n:5}")
    private int rerankTopN;

    public ChatService(HybridSearchService hybridSearchService,
                       RerankService rerankService,
                       TokenBudgetManager tokenBudgetManager,
                       AnswerGenerator answerGenerator,
                       CitationFormatter citationFormatter,
                       @Qualifier("searchExecutor") Executor searchExecutor) {
        this.hybridSearchService = hybridSearchService;
        this.rerankService = rerankService;
        this.tokenBudgetManager = tokenBudgetManager;
        this.answerGenerator = answerGenerator;
        this.citationFormatter = citationFormatter;
        this.searchExecutor = searchExecutor;
    }

    /** 检索管线的产出：格式化后的上下文 + 引用列表 */
    private record PreparedContext(String context, List<CitationDto> citations) {}

    /**
     * 公共检索管线：混合检索 → 重排 → Token 预算 → 上下文格式化 → 引用组装。
     *
     * @return 准备好的上下文；检索无候选时返回 {@code null}
     */
    private PreparedContext prepareContext(String question, List<UUID> knowledgeBaseIds) {
        List<KbChunk> candidates = hybridSearchService.search(question, knowledgeBaseIds);
        log.info("混合检索返回 {} 个候选", candidates.size());
        if (candidates.isEmpty()) {
            return null;
        }

        List<KbChunk> reranked = rerankService.rerank(question, candidates, rerankTopN);
        log.info("重排后保留 {} 个最终上下文切片", reranked.size());

        List<TokenBudgetManager.ChunkBudgetEntry> budgetEntries =
                tokenBudgetManager.fitToBudget(reranked);
        log.info("Token 预算：适配 {} 个切片", budgetEntries.size());

        String context = citationFormatter.formatContextForPrompt(budgetEntries);

        List<KbChunk> finalKbChunks = budgetEntries.stream()
                .map(TokenBudgetManager.ChunkBudgetEntry::kbChunk)
                .toList();
        return new PreparedContext(context, citationFormatter.buildCitations(finalKbChunks));
    }

    /**
     * 端到端处理一次聊天请求（同步）。
     *
     * @param question 用户问题
     * @return 包含答案与引用的 ChatResponse
     */
    public ChatResponse chat(String question, List<UUID> knowledgeBaseIds) {
        log.info("处理聊天请求：{}", question);

        PreparedContext prepared = prepareContext(question, knowledgeBaseIds);
        if (prepared == null) {
            return new ChatResponse(NO_RESULT_ANSWER, List.of());
        }

        String answer = answerGenerator.generate(question, prepared.context());

        log.info("聊天响应已生成，包含 {} 条引用", prepared.citations().size());
        return new ChatResponse(answer, prepared.citations());
    }

    /**
     * 通过 SSE 流式返回聊天响应。
     *
     * <p>事件协议（浏览器通过 {@code EventSource} 消费）：</p>
     * <ul>
     *   <li>{@code event: token} — 数据为答案的一个小文本片段</li>
     *   <li>{@code event: done}  — 数据为最终的 {@link ChatResponse} JSON
     *       （完整答案 + 引用），随后流结束</li>
     *   <li>{@code event: error} — 数据为错误信息字符串</li>
     * </ul>
     *
     * <p>检索 / 重排 / token 预算流水线在异步工作线程中同步执行，
     * 仅 LLM 答案生成部分采用流式输出。</p>
     *
     * @param question 用户问题
     * @return 客户端订阅的已开启的 {@link SseEmitter}
     */
    public SseEmitter streamChat(String question, List<UUID> knowledgeBaseIds) {
        log.info("处理流式聊天请求：{}", question);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // 超时 / 客户端断开时清理资源
        emitter.onTimeout(() -> {
            log.warn("SSE 超时，问题：{}", question);
            emitter.complete();
        });
        emitter.onError(ex -> log.error("SSE 出错，问题：{}", question, ex));

        // 检索流水线在检索线程池中异步执行，避免 ForkJoinPool.commonPool 争用
        CompletableFuture.runAsync(() -> {
            try {
                // ---------- 阶段一：公共检索管线（同步） ----------
                PreparedContext prepared = prepareContext(question, knowledgeBaseIds);
                if (prepared == null) {
                    sendEvent(emitter, "done", new ChatResponse(NO_RESULT_ANSWER, List.of()));
                    emitter.complete();
                    return;
                }

                // ---------- 阶段二：逐 token 流式生成答案 ----------
                StringBuilder fullAnswer = new StringBuilder();

                answerGenerator.streamGenerate(question, prepared.context())
                        .doOnNext(token -> {
                            try {
                                fullAnswer.append(token);
                                sendEvent(emitter, "token", token);
                            } catch (IOException e) {
                                log.warn("发送 token 片段时 IO 出错", e);
                                throw new RuntimeException(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                ChatResponse finalResponse = new ChatResponse(
                                        fullAnswer.toString(), prepared.citations());
                                sendEvent(emitter, "done", finalResponse);
                                emitter.complete();
                                log.info("流式聊天完成：{} 字符，{} 条引用",
                                        fullAnswer.length(), prepared.citations().size());
                            } catch (IOException e) {
                                log.warn("发送 done 事件时 IO 出错", e);
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnError(e -> {
                            log.error("流式生成失败", e);
                            try {
                                sendEvent(emitter, "error",
                                        "生成答案时出现错误：" + e.getMessage());
                            } catch (IOException ignored) {
                                // 尽力而为
                            }
                            emitter.completeWithError(e);
                        })
                        .subscribe();

            } catch (Exception e) {
                log.error("流式聊天流水线失败", e);
                try {
                    sendEvent(emitter, "error", "处理请求时出现错误：" + e.getMessage());
                } catch (IOException ignored) {
                    // 尽力而为
                }
                emitter.completeWithError(e);
            }
        }, searchExecutor);

        return emitter;
    }

    /**
     * 发送单个 SSE 事件，数据以 JSON 序列化。
     */
    private void sendEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
    }
}
