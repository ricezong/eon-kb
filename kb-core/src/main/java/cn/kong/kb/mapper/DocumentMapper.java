package cn.kong.kb.mapper;

import cn.kong.kb.domain.DocumentStatus;
import cn.kong.kb.domain.KbDocument;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/**
 * documents 表的 MyBatis Mapper。
 * 所有 SQL 均位于 resources/mapper/DocumentMapper.xml。
 */
public interface DocumentMapper {

    /**
     * 插入新的文档记录。生成的 UUID 主键会回写到 document.id。
     */
    int insert(KbDocument kbDocument);

    int updateStatus(@Param("id") UUID id,
                     @Param("status") DocumentStatus status,
                     @Param("errorMessage") String errorMessage);

    int updateChunkCount(@Param("id") UUID id, @Param("chunkCount") int chunkCount);

    KbDocument findById(@Param("id") UUID id);

    List<KbDocument> findAll();

    /**
     * 查询某个知识库下的所有文档。
     */
    List<KbDocument> findByKnowledgeBaseId(@Param("knowledgeBaseId") UUID knowledgeBaseId);

    int deleteById(@Param("id") UUID id);
}
