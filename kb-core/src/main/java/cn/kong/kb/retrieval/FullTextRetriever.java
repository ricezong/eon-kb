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
 * 全文检索器，使用中文 tsvector 与 ts_rank 进行匹配。
 */
@Component
public class FullTextRetriever {

    private static final Logger log = LoggerFactory.getLogger(FullTextRetriever.class);

    private final ChunkMapper chunkMapper;

    @Value("${app.retrieval.fulltext-top-k:20}")
    private int topK;

    public FullTextRetriever(ChunkMapper chunkMapper) {
        this.chunkMapper = chunkMapper;
    }

    /**
     * 通过中文分词的全文检索获取 Top-K 个切片。
     *
     * @param query 原始查询（用于关键词匹配）
     * @return 匹配到的切片列表，包含 ts_rank 分数
     */
    public List<KbChunk> retrieve(String query, List<UUID> knowledgeBaseIds) {
        List<KbChunk> results = chunkMapper.searchByFullText(query, topK, knowledgeBaseIds);
        log.debug("全文检索：查询返回 {} 条结果", results.size());
        return results;
    }
}
