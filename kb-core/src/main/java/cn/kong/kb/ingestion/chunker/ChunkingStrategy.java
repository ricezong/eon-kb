package cn.kong.kb.ingestion.chunker;

import cn.kong.kb.domain.KbChunk;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.UUID;

/**
 * 文档分块策略接口。
 */
public interface ChunkingStrategy {

    /**
     * 依据策略将文档切分为多个切片。
     *
     * @param documents 已解析的文档列表（包含内容与元数据）
     * @param documentId 所属父文档 ID
     * @return 包含内容与元数据的 Chunk 对象列表
     */
    List<KbChunk> chunk(List<Document> documents, UUID documentId);
}
