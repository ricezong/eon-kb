package cn.kong.kb.mapper;

import cn.kong.kb.domain.KnowledgeBase;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/**
 * knowledge_bases 表及关联表的 MyBatis Mapper。
 * 所有 SQL 均位于 resources/mapper/KnowledgeBaseMapper.xml。
 */
public interface KnowledgeBaseMapper {

    /**
     * 插入新的知识库记录。主键通过 useGeneratedKeys 回写。
     */
    int insert(KnowledgeBase kb);

    int update(@Param("id") UUID id,
               @Param("name") String name,
               @Param("description") String description,
               @Param("color") String color);

    KnowledgeBase findById(@Param("id") UUID id);

    List<KnowledgeBase> findAll();

    int deleteById(@Param("id") UUID id);

    // ==================== 文档-知识库关联操作 ====================

    /**
     * 将文档关联到指定知识库（幂等操作）。
     */
    int assignDocument(@Param("documentId") UUID documentId,
                       @Param("knowledgeBaseId") UUID knowledgeBaseId);

    /**
     * 移除文档的所有知识库关联。
     */
    int removeAllForDocument(@Param("documentId") UUID documentId);

    /**
     * 查询文档归属的所有知识库。
     */
    List<KnowledgeBase> findByDocumentId(@Param("documentId") UUID documentId);

    /**
     * 查询文档归属的知识库 ID 列表。
     */
    List<UUID> findKnowledgeBaseIdsByDocumentId(@Param("documentId") UUID documentId);
}
