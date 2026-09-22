package cn.kong.kb.mapper;

import cn.kong.kb.domain.KbChunk;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;


/**
 * chunks 表的 MyBatis Mapper。
 * 所有 SQL 均位于 resources/mapper/ChunkMapper.xml。
 */
public interface ChunkMapper {

    /**
     * 基于 PgVector 余弦距离的向量相似度检索。
     *
     * @param embeddingVector embedding 的 PG vector 字面量字符串，例如 "[0.1,0.2,...]"
     * @param topK 返回数量上限
     * @return 匹配到的切片列表，similarity_score 已填充
     */
    List<KbChunk> searchByVector(@Param("embeddingVector") String embeddingVector,
                                 @Param("topK") int topK,
                                 @Param("knowledgeBaseIds") List<UUID> knowledgeBaseIds);

    /**
     * 使用中文 tsvector 进行全文检索。
     */
    List<KbChunk> searchByFullText(@Param("query") String query,
                                   @Param("topK") int topK,
                                   @Param("knowledgeBaseIds") List<UUID> knowledgeBaseIds);

    /**
     * 使用 pg_trgm 相似度进行模糊匹配。
     */
    List<KbChunk> searchByFuzzy(@Param("query") String query,
                                @Param("topK") int topK,
                                @Param("knowledgeBaseIds") List<UUID> knowledgeBaseIds);

    KbChunk findById(@Param("id") UUID id);

    List<KbChunk> findByDocumentId(@Param("documentId") UUID documentId);

    /**
     * 单条语句批量填充切片的业务列（document_id / chunk_index / chunk_type / title）。
     *
     * <p>切片主键在摄入时预设（同时作为向量表主键），因此填充与向量插入
     * 由同一 ID 关联，不再需要逐条回写，也没有并发竞态窗口。</p>
     */
    int fillChunkMetadata(@Param("chunks") List<KbChunk> chunks);

    /**
     * 按主键批量删除切片（用于向量化失败后的补偿清理）。
     */
    int deleteByIds(@Param("ids") List<UUID> ids);

    /**
     * 查询切片所属文档的 file_name（联表查询）。
     */
    String findDocumentNameByChunkId(@Param("chunkId") UUID chunkId);
}
