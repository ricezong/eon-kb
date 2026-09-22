package cn.kong.kb.ingestion.parser;

import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档解析接口。
 * 各实现类应负责从不同格式的文档中提取文本与元数据。
 */
public interface DocumentParser {

    /**
     * 解析文件并返回 Document 对象列表。
     *
     * @param file 上传的文件
     * @return 包含内容与元数据的 Document 对象列表
     */
    List<Document> parse(MultipartFile file);
}
