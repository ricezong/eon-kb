package cn.kong.kb.api;

import cn.kong.kb.api.dto.AssignKnowledgeBasesRequest;
import cn.kong.kb.api.dto.DocumentDto;
import cn.kong.kb.api.dto.KnowledgeBaseDto;
import cn.kong.kb.domain.DocumentStatus;
import cn.kong.kb.domain.KbDocument;
import cn.kong.kb.domain.KbDocumentType;
import cn.kong.kb.domain.KnowledgeBase;
import cn.kong.kb.ingestion.DocumentIngestionService;
import cn.kong.kb.mapper.DocumentMapper;
import cn.kong.kb.mapper.KnowledgeBaseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentIngestionService ingestionService;
    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public DocumentController(DocumentIngestionService ingestionService,
                               DocumentMapper documentMapper,
                               KnowledgeBaseMapper knowledgeBaseMapper) {
        this.ingestionService = ingestionService;
        this.documentMapper = documentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    /**
     * 上传文档以进行处理。
     *
     * <p>文件类型白名单统一由 {@link KbDocumentType#isSupportedFileName} 判定
     * （扩展名登记表唯一维护，此处不再保留一份私有白名单）。</p>
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "knowledgeBaseIds", required = false) List<UUID> knowledgeBaseIds) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
        }

        // 文件类型白名单校验（fail-closed，与解析路由共享同一登记表）
        String fileName = file.getOriginalFilename();
        if (!KbDocumentType.isSupportedFileName(fileName)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Unsupported file type. Allowed: PDF, Word, PPT, Excel, TXT, Markdown, HTML"));
        }

        log.info("上传文档：{}（{} 字节）", fileName, file.getSize());
        UUID documentId = ingestionService.ingestDocument(file);

        // 保存知识库关联
        if (knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty()) {
            for (UUID kbId : knowledgeBaseIds) {
                knowledgeBaseMapper.assignDocument(documentId, kbId);
            }
            log.info("文档 {} 已关联 {} 个知识库", documentId, knowledgeBaseIds.size());
        }

        return ResponseEntity.ok(Map.of(
                "documentId", documentId.toString(),
                "fileName", fileName,
                "status", DocumentStatus.PROCESSING.name(),
                "message", "Document uploaded and processing started"
        ));
    }

    /**
     * 列出所有文档。
     */
    @GetMapping
    public ResponseEntity<List<DocumentDto>> list() {
        List<DocumentDto> docs = documentMapper.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(docs);
    }

    /**
     * 设置文档的知识库归属（替换现有归属）。
     *
     * <p>"先删后插"的多语句操作，在 kb 事务管理器中原子执行：
     * 中途失败会整体回滚，避免归属被清空却未重建。</p>
     */
    @PutMapping("/{id}/knowledge-bases")
    @Transactional("kbTransactionManager")
    public ResponseEntity<?> assignKnowledgeBases(
            @PathVariable UUID id,
            @RequestBody AssignKnowledgeBasesRequest request) {
        KbDocument doc = documentMapper.findById(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        // 先移除所有现有归属
        knowledgeBaseMapper.removeAllForDocument(id);
        // 再设置新归属
        if (request.knowledgeBaseIds() != null) {
            for (String kbIdStr : request.knowledgeBaseIds()) {
                knowledgeBaseMapper.assignDocument(id, UUID.fromString(kbIdStr));
            }
        }
        return ResponseEntity.ok(Map.of("message", "Knowledge bases assigned"));
    }

    /**
     * 获取文档的知识库归属。
     */
    @GetMapping("/{id}/knowledge-bases")
    public ResponseEntity<List<KnowledgeBaseDto>> getKnowledgeBases(@PathVariable UUID id) {
        List<KnowledgeBaseDto> kbList = knowledgeBaseMapper.findByDocumentId(id).stream()
                .map(this::toKbDto)
                .toList();
        return ResponseEntity.ok(kbList);
    }

    /**
     * 根据 ID 获取文档。
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> getById(@PathVariable UUID id) {
        KbDocument doc = documentMapper.findById(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(doc));
    }

    /**
     * 删除文档及其所有切片。
     *
     * <p>切片行（含向量与全文索引内容）由数据库外键
     * {@code kb_chunks.document_id REFERENCES documents ON DELETE CASCADE}
     * 级联清理，应用层无需逐条删除——该级联依赖见 db/schema.sql。</p>
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        KbDocument doc = documentMapper.findById(id);
        if (doc == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Document not found"));
        }
        documentMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted"));
    }

    private DocumentDto toDto(KbDocument doc) {
        List<KnowledgeBase> kbList = knowledgeBaseMapper.findByDocumentId(doc.getId());
        List<KnowledgeBaseDto> kbDtos = kbList.stream().map(this::toKbDto).toList();
        return new DocumentDto(
                doc.getId().toString(),
                doc.getFileName(),
                doc.getFileType(),
                doc.getFileSize(),
                doc.getChunkCount(),
                doc.getStatus() != null ? doc.getStatus().name() : null,
                doc.getErrorMessage(),
                doc.getCreatedAt(),
                kbDtos
        );
    }

    private KnowledgeBaseDto toKbDto(KnowledgeBase kb) {
        return new KnowledgeBaseDto(
                kb.getId() != null ? kb.getId().toString() : null,
                kb.getName(),
                kb.getDescription(),
                kb.getColor(),
                kb.getDocumentCount(),
                kb.getCreatedAt()
        );
    }
}
