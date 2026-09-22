package cn.kong.kb.api;

import cn.kong.kb.api.dto.ChunkDto;
import cn.kong.kb.domain.KbChunk;
import cn.kong.kb.mapper.ChunkMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chunks")
public class ChunkController {

    private final ChunkMapper chunkMapper;

    public ChunkController(ChunkMapper chunkMapper) {
        this.chunkMapper = chunkMapper;
    }

    /**
     * 根据切片 ID 获取切片详情（用于预览 / 查看）。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChunkDto> getChunk(@PathVariable UUID id) {
        KbChunk kbChunk = chunkMapper.findById(id);
        if (kbChunk == null) {
            return ResponseEntity.notFound().build();
        }

        String documentName = chunkMapper.findDocumentNameByChunkId(id);
        if (documentName == null) {
            documentName = "Unknown";
        }

        return ResponseEntity.ok(new ChunkDto(
                kbChunk.getId().toString(),
                kbChunk.getContent(),
                kbChunk.getDocumentId() != null ? kbChunk.getDocumentId().toString() : "",
                documentName,
                kbChunk.getChunkIndex(),
                kbChunk.getChunkType() != null ? kbChunk.getChunkType() : "",
                kbChunk.getTitle() != null ? kbChunk.getTitle() : "",
                kbChunk.getMetadata() != null ? kbChunk.getMetadata() : Map.of()
        ));
    }

    /**
     * 根据文档 ID 列出该文档的所有切片。
     */
    @GetMapping
    public ResponseEntity<List<ChunkDto>> listByDocument(@RequestParam UUID documentId) {
        List<ChunkDto> result = chunkMapper.findByDocumentId(documentId).stream()
                .map(chunk -> new ChunkDto(
                        chunk.getId().toString(),
                        chunk.getContent(),
                        chunk.getDocumentId() != null ? chunk.getDocumentId().toString() : "",
                        chunk.getChunkIndex(),
                        chunk.getChunkType() != null ? chunk.getChunkType() : "",
                        chunk.getTitle() != null ? chunk.getTitle() : ""
                ))
                .toList();
        return ResponseEntity.ok(result);
    }
}
