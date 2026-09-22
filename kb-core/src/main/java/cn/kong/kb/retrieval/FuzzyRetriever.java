package cn.kong.kb.retrieval;

import cn.kong.kb.domain.KbChunk;
import cn.kong.kb.mapper.ChunkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 模糊检索器，使用 pg_trgm 相似度进行匹配。
 * 可捕获拼写错误及近似匹配。
 */
@Component
public class FuzzyRetriever {

    private static final Logger log = LoggerFactory.getLogger(FuzzyRetriever.class);

    private final ChunkMapper chunkMapper;

    @Value("${app.retrieval.fuzzy-top-k:10}")
    private int topK;

    public FuzzyRetriever(ChunkMapper chunkMapper) {
        this.chunkMapper = chunkMapper;
    }

    /**
     * 通过三元组（trigram）相似度获取 Top-K 个切片。
     *
     * @param query 原始查询
     * @return 匹配到的切片列表，包含三元组相似度分数
     */
    public List<KbChunk> retrieve(String query, List<UUID> knowledgeBaseIds) {
        List<KbChunk> results = chunkMapper.searchByFuzzy(query, topK, knowledgeBaseIds);
        log.debug("模糊检索：查询返回 {} 条结果", results.size());
        return results;
    }
}
