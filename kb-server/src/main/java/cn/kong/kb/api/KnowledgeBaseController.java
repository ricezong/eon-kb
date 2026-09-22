package cn.kong.kb.api;

import cn.kong.kb.api.dto.CreateKnowledgeBaseRequest;
import cn.kong.kb.api.dto.DocumentDto;
import cn.kong.kb.api.dto.KnowledgeBaseDto;
import cn.kong.kb.api.dto.UpdateKnowledgeBaseRequest;
import cn.kong.kb.domain.KbDocument;
import cn.kong.kb.domain.KnowledgeBase;
import cn.kong.kb.mapper.DocumentMapper;
import cn.kong.kb.mapper.KnowledgeBaseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;

    public KnowledgeBaseController(KnowledgeBaseMapper knowledgeBaseMapper,
                                    DocumentMapper documentMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.documentMapper = documentMapper;
    }

    /**
     * 创建知识库。
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateKnowledgeBaseRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "知识库名称不能为空"));
        }
        String description = request.description() != null ? request.description() : "";
        String color = request.color() != null ? request.color() : "#3b82f6";

        KnowledgeBase kb = new KnowledgeBase(request.name().trim(), description, color);
        knowledgeBaseMapper.insert(kb);
        log.info("创建知识库：{} ({})", request.name(), kb.getId());

        return ResponseEntity.ok(toDto(kb));
    }

    /**
     * 列出全部知识库。
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeBaseDto>> list() {
        List<KnowledgeBaseDto> list = knowledgeBaseMapper.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * 获取单个知识库。
     */
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBaseDto> getById(@PathVariable UUID id) {
        KnowledgeBase kb = knowledgeBaseMapper.findById(id);
        if (kb == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(kb));
    }

    /**
     * 更新知识库。请求中为 {@code null} 的字段表示保持原值。
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id,
                                    @RequestBody UpdateKnowledgeBaseRequest request) {
        KnowledgeBase existing = knowledgeBaseMapper.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        String name = request.name() != null ? request.name() : existing.getName();
        String description = request.description() != null ? request.description() : existing.getDescription();
        String color = request.color() != null ? request.color() : existing.getColor();

        knowledgeBaseMapper.update(id, name, description, color);
        log.info("更新知识库：{} ({})", name, id);

        return ResponseEntity.ok(toDto(knowledgeBaseMapper.findById(id)));
    }

    /**
     * 删除知识库。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        KnowledgeBase kb = knowledgeBaseMapper.findById(id);
        if (kb == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "知识库不存在"));
        }
        knowledgeBaseMapper.deleteById(id);
        log.info("删除知识库：{} ({})", kb.getName(), id);
        return ResponseEntity.ok(Map.of("message", "知识库已删除"));
    }

    /**
     * 获取知识库下的文档列表。
     */
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<DocumentDto>> documents(@PathVariable UUID id) {
        List<DocumentDto> docs = documentMapper.findByKnowledgeBaseId(id).stream()
                .map(this::toDocumentDto)
                .toList();
        return ResponseEntity.ok(docs);
    }

    private KnowledgeBaseDto toDto(KnowledgeBase kb) {
        return new KnowledgeBaseDto(
                kb.getId() != null ? kb.getId().toString() : null,
                kb.getName(),
                kb.getDescription(),
                kb.getColor(),
                kb.getDocumentCount(),
                kb.getCreatedAt()
        );
    }

    private DocumentDto toDocumentDto(KbDocument doc) {
        List<KnowledgeBase> kbList = knowledgeBaseMapper.findByDocumentId(doc.getId());
        List<KnowledgeBaseDto> kbDtos = kbList.stream().map(this::toDto).toList();
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
}
