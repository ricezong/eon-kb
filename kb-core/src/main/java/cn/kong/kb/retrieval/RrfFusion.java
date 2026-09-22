package cn.kong.kb.retrieval;

import cn.kong.kb.domain.KbChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 倒数排名融合（RRF）算法，用于合并多个已排序的结果列表。
 * 公式：RRF_score(d) = SUM(1 / (k + rank_i(d)))
 * 其中 k 为常数（通常取 60）。
 */
@Component
public class RrfFusion {

    @Value("${app.retrieval.rrf-k:60}")
    private int k;

    @Value("${app.retrieval.fusion-top-k:10}")
    private int topK;

    /**
     * 使用 RRF 算法融合多个已排序的切片列表。
     *
     * @param rankedLists 多个已排序的切片列表（来自不同的检索方法）
     * @return 融合并重新排序后的 Top-K 切片列表
     */
    public List<KbChunk> fuse(List<List<KbChunk>> rankedLists) {
        Map<UUID, KbChunk> chunkMap = new HashMap<>();
        Map<UUID, Double> rrfScores = new HashMap<>();

        for (List<KbChunk> rankedList : rankedLists) {
            for (int rank = 0; rank < rankedList.size(); rank++) {
                KbChunk kbChunk = rankedList.get(rank);
                UUID chunkId = kbChunk.getId();

                // 计算 RRF 分数贡献：1 / (k + rank)
                // rank 从 0 开始计数，故加 1 转换为从 1 开始的排名
                double contribution = 1.0 / (k + rank + 1);

                rrfScores.merge(chunkId, contribution, Double::sum);
                chunkMap.putIfAbsent(chunkId, kbChunk);
            }
        }

        // 按 RRF 分数降序排序
        List<KbChunk> fusedResults = new ArrayList<>();
        rrfScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(topK)
                .forEach(entry -> {
                    KbChunk kbChunk = chunkMap.get(entry.getKey());
                    kbChunk.setScore(entry.getValue());
                    fusedResults.add(kbChunk);
                });

        return fusedResults;
    }
}
