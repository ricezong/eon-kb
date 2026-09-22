package cn.kong.kb.retrieval;

import cn.kong.kb.domain.KbChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 混合检索编排服务：查询预处理 → 并行检索 → RRF 融合。
 */
@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);
    private static final int TIMEOUT_SECONDS = 5;

    private final QueryPreprocessor queryPreprocessor;
    private final VectorRetriever vectorRetriever;
    private final FullTextRetriever fullTextRetriever;
    private final FuzzyRetriever fuzzyRetriever;
    private final RrfFusion rrfFusion;
    private final Executor searchExecutor;

    public HybridSearchService(QueryPreprocessor queryPreprocessor,
                                VectorRetriever vectorRetriever,
                                FullTextRetriever fullTextRetriever,
                                FuzzyRetriever fuzzyRetriever,
                                RrfFusion rrfFusion,
                                @Qualifier("searchExecutor") Executor searchExecutor) {
        this.queryPreprocessor = queryPreprocessor;
        this.vectorRetriever = vectorRetriever;
        this.fullTextRetriever = fullTextRetriever;
        this.fuzzyRetriever = fuzzyRetriever;
        this.rrfFusion = rrfFusion;
        this.searchExecutor = searchExecutor;
    }

    /**
     * 执行混合检索流水线。
     *
     * @param originalQuery 用户原始查询
     * @return 融合后的 Top-K 候选切片
     */
    public List<KbChunk> search(String originalQuery, List<UUID> knowledgeBaseIds) {
        log.info("开始混合检索：{}", originalQuery);

        // 第一步：预处理查询（仅向量检索使用处理后的查询）
        String processedQuery = queryPreprocessor.preprocess(originalQuery);

        // 第二步：并行检索（使用检索线程池，避免 ForkJoinPool.commonPool 争用）
        // 全文/模糊检索直接使用原始查询——其中包含用户真正关注的关键词
        CompletableFuture<List<KbChunk>> vectorFuture = CompletableFuture.supplyAsync(
                () -> vectorRetriever.retrieve(processedQuery, knowledgeBaseIds), searchExecutor);
        CompletableFuture<List<KbChunk>> fullTextFuture = CompletableFuture.supplyAsync(
                () -> fullTextRetriever.retrieve(originalQuery, knowledgeBaseIds), searchExecutor);
        CompletableFuture<List<KbChunk>> fuzzyFuture = CompletableFuture.supplyAsync(
                () -> fuzzyRetriever.retrieve(originalQuery, knowledgeBaseIds), searchExecutor);

        try {
            CompletableFuture.allOf(vectorFuture, fullTextFuture, fuzzyFuture)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            List<KbChunk> vectorResults = vectorFuture.get();
            List<KbChunk> fullTextResults = fullTextFuture.get();
            List<KbChunk> fuzzyResults = fuzzyFuture.get();

            log.info("检索结果 - 向量：{}，全文：{}，模糊：{}",
                    vectorResults.size(), fullTextResults.size(), fuzzyResults.size());

            // 第三步：RRF 融合
            List<KbChunk> fusedResults = rrfFusion.fuse(List.of(vectorResults, fullTextResults, fuzzyResults));
            log.info("RRF 融合产出 {} 个候选", fusedResults.size());

            return fusedResults;

        } catch (Exception e) {
            log.error("混合检索失败", e);
            // 兜底：仅尝试向量检索
            try {
                return vectorRetriever.retrieve(processedQuery, knowledgeBaseIds);
            } catch (Exception ex) {
                log.error("向量检索兜底也失败", ex);
                return List.of();
            }
        }
    }
}
