package cn.kong.kb.ingestion.parser;

import cn.kong.kb.ingestion.DocumentSource;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 文档解析接口。
 * 各实现类应负责从不同格式的文档中提取文本与元数据。
 *
 * <p>输入统一为 {@link DocumentSource}（文件名 + 字节的不可变快照），
 * 不依赖 Spring Web 的 {@code MultipartFile}，可安全在异步线程中使用。</p>
 */
public interface DocumentParser {

    /**
     * 解析文档并返回 Document 对象列表。
     *
     * @param source 文档输入快照
     * @return 包含内容与元数据的 Document 对象列表
     */
    List<Document> parse(DocumentSource source);
}
