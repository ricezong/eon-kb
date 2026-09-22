package cn.kong.kb.retrieval;

import cn.kong.kb.domain.KbChunk;
import cn.kong.kb.mapper.ChunkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 基于向量的检索器，使用 embedding 的余弦相似度进行匹配。
 */
@Component
public class VectorRetriever {

    private static final Logger log = LoggerFactory.getLogger(VectorRetriever.class);

    private final EmbeddingModel embeddingModel;
    private final ChunkMapper chunkMapper;

    @Value("${app.retrieval.vector-top-k:20}")
    private int topK;

    public VectorRetriever(EmbeddingModel embeddingModel, ChunkMapper chunkMapper) {
        this.embeddingModel = embeddingModel;
        this.chunkMapper = chunkMapper;
    }

    /**
     * 通过向量相似度检索 Top-K 个切片。
     *
     * @param processedQuery 预处理后的查询文本（HyDE 生成或改写后的文本）
     * @return 匹配到的切片列表，包含相似度分数
     */
    public List<KbChunk> retrieve(String processedQuery, List<UUID> knowledgeBaseIds) {
        float[] embedding = embeddingModel.embed(processedQuery);
        String vectorLiteral = toPgVectorLiteral(embedding);
        List<KbChunk> results = chunkMapper.searchByVector(vectorLiteral, topK, knowledgeBaseIds);
        log.debug("向量检索：查询返回 {} 条结果", results.size());
        return results;
    }

    /**
     * 将 float[] 形式的 embedding 转换为 PgVector 字面量字符串。
     * <p>格式示例：[0.123,0.456,0.789]</p>
     *
     * @param embedding embedding 数组
     * @return PgVector 字面量字符串
     */
    private String toPgVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
