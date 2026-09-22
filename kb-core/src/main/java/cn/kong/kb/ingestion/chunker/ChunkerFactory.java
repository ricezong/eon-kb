package cn.kong.kb.ingestion.chunker;

import cn.kong.kb.domain.KbChunk;
import cn.kong.kb.domain.KbDocumentType;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 分块策略工厂，根据文档类型选择对应的分块策略。
 */
@Component
public class ChunkerFactory {

    private final HeadingChunker headingChunker;
    private final SlideChunker slideChunker;
    private final SheetChunker sheetChunker;
    private final ParagraphChunker paragraphChunker;

    public ChunkerFactory(HeadingChunker headingChunker, SlideChunker slideChunker,
                          SheetChunker sheetChunker, ParagraphChunker paragraphChunker) {
        this.headingChunker = headingChunker;
        this.slideChunker = slideChunker;
        this.sheetChunker = sheetChunker;
        this.paragraphChunker = paragraphChunker;
    }

    public ChunkingStrategy getStrategy(KbDocumentType type) {
        return switch (type) {
            case PDF, DOCX -> headingChunker;
            case PPTX -> slideChunker;
            case XLSX -> sheetChunker;
            // HTML 经 HtmlToMarkdownConverter 转为 Markdown 后，与 MD 同路分块
            case TXT, MD, HTML -> paragraphChunker;
        };
    }

    public List<KbChunk> chunk(List<Document> documents,
                               UUID documentId, KbDocumentType type) {
        return getStrategy(type).chunk(documents, documentId);
    }
}
