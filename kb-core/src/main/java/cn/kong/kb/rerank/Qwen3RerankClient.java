package cn.kong.kb.rerank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.kong.kb.domain.KbChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * 百炼 qwen3-rerank 接口客户端。
 * 发送查询 + 文档文本，获取带相关性分数的重排结果。
 */
@Component
public class Qwen3RerankClient implements RerankService {

    private static final Logger log = LoggerFactory.getLogger(Qwen3RerankClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;

    @Value("${app.rerank.model:qwen3-rerank}")
    private String model;

    @Value("${app.rerank.top-n:5}")
    private int defaultTopN;

    public Qwen3RerankClient(@Qualifier("rerankRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<KbChunk> rerank(String query, List<KbChunk> candidates, int topN) {
        if (candidates.isEmpty()) return List.of();

        try {
            // 构建请求体
            List<String> documents = candidates.stream()
                    .map(KbChunk::getContent)
                    .toList();

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("query", query);
            requestBody.put("documents", documents);
            requestBody.put("top_n", topN);

            String requestJson = OBJECT_MAPPER.writeValueAsString(requestBody);

            // 调用接口
            String responseJson = restClient.post()
                    .uri("/reranks")
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            // 解析响应
            JsonNode response = OBJECT_MAPPER.readTree(responseJson);
            JsonNode results = response.get("results");

            if (results == null || results.isEmpty()) {
                log.warn("Rerank 接口未返回结果");
                return new ArrayList<>(candidates.subList(0, Math.min(topN, candidates.size())));
            }

            // 将结果映射回切片
            List<KbChunk> reranked = new ArrayList<>();
            for (JsonNode result : results) {
                int index = result.get("index").asInt();
                double score = result.get("relevance_score").asDouble();

                if (index >= 0 && index < candidates.size()) {
                    KbChunk kbChunk = candidates.get(index);
                    kbChunk.setScore(score);
                    reranked.add(kbChunk);
                }
            }

            log.info("将 {} 个候选重排为 {} 个结果", candidates.size(), reranked.size());
            return reranked;

        } catch (Exception e) {
            log.error("Rerank 接口调用失败，直接返回顶部候选", e);
            return new ArrayList<>(candidates.subList(0, Math.min(topN, candidates.size())));
        }
    }
}
