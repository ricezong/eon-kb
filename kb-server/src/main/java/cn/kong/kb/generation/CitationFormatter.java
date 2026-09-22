package cn.kong.kb.generation;

import cn.kong.kb.api.dto.CitationDto;
import cn.kong.kb.domain.KbChunk;
import cn.kong.kb.mapper.ChunkMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 引用格式化器，基于检索到的切片构建引用来源。
 */
@Component
public class CitationFormatter {

    private final ChunkMapper chunkMapper;

    public CitationFormatter(ChunkMapper chunkMapper) {
        this.chunkMapper = chunkMapper;
    }

    /**
     * 根据切片列表构建引用列表。
     */
    public List<CitationDto> buildCitations(List<KbChunk> kbChunks) {
        List<CitationDto> citations = new ArrayList<>();

        for (int i = 0; i < kbChunks.size(); i++) {
            KbChunk kbChunk = kbChunks.get(i);
            String documentName = chunkMapper.findDocumentNameByChunkId(kbChunk.getId());
            if (documentName == null) {
                documentName = "Unknown";
            }

            String preview = kbChunk.getContent();
            if (preview != null && preview.length() > 150) {
                preview = preview.substring(0, 150) + "...";
            }

            citations.add(new CitationDto(
                    kbChunk.getId().toString(),
                    i + 1,
                    documentName,
                    kbChunk.getChunkType(),
                    kbChunk.getTitle(),
                    preview,
                    kbChunk.getScore()
            ));
        }

        return citations;
    }

    /**
     * 将上下文片段按编号引用格式拼接，供 prompt 使用。
     */
    public String formatContextForPrompt(List<TokenBudgetManager.ChunkBudgetEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            TokenBudgetManager.ChunkBudgetEntry entry = entries.get(i);
            sb.append("[").append(i + 1).append("] ");
            if (entry.kbChunk().getTitle() != null && !entry.kbChunk().getTitle().isBlank()) {
                sb.append("(").append(entry.kbChunk().getTitle()).append(") ");
            }
            sb.append(entry.adjustedContent()).append("\n\n");
        }
        return sb.toString();
    }
}
