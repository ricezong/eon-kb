package cn.kong.kb.ingestion.parser;

import ai.llamaindex.llamacloud.client.LlamaCloudClient;
import ai.llamaindex.llamacloud.core.MultipartField;
import ai.llamaindex.llamacloud.errors.LlamaCloudIoException;
import ai.llamaindex.llamacloud.errors.LlamaCloudServiceException;
import ai.llamaindex.llamacloud.models.files.FileCreateParams;
import ai.llamaindex.llamacloud.models.files.FileCreateResponse;
import ai.llamaindex.llamacloud.models.parsing.ParsingCreateParams;
import ai.llamaindex.llamacloud.models.parsing.ParsingCreateResponse;
import ai.llamaindex.llamacloud.models.parsing.ParsingGetParams;
import ai.llamaindex.llamacloud.models.parsing.ParsingGetResponse;
import cn.kong.kb.config.LlamaParseProperties;
import cn.kong.kb.domain.KbDocumentType;
import cn.kong.kb.ingestion.DocumentSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LlamaParse 云端文档解析器（LlamaCloud 官方 Java SDK）。
 *
 * <p>流程：上传文件（purpose=parse）→ 创建解析任务（tier + LATEST 版本）→
 * 轮询任务直到终态（COMPLETED / FAILED / CANCELLED）→ 提取按页 Markdown。</p>
 *
 * <p>仅在 {@code app.parsing.llamaparse.enabled=true} 时装配；
 * 云端失败时由 {@link DocumentParserRouter} 回退本地解析。</p>
 */
@Component
@ConditionalOnProperty(name = "app.parsing.llamaparse.enabled", havingValue = "true")
public class LlamaParseDocumentParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(LlamaParseDocumentParser.class);

    private final LlamaCloudClient client;
    private final LlamaParseProperties properties;
    private final HtmlToMarkdownConverter htmlToMarkdownConverter;

    public LlamaParseDocumentParser(LlamaCloudClient client,
                                    LlamaParseProperties properties,
                                    HtmlToMarkdownConverter htmlToMarkdownConverter) {
        this.client = client;
        this.properties = properties;
        this.htmlToMarkdownConverter = htmlToMarkdownConverter;
    }

    /**
     * 当前文档类型是否走云端解析（由配置 {@code app.parsing.llamaparse.types} 决定）。
     */
    public boolean supports(KbDocumentType type) {
        return properties.getTypes().contains(type);
    }

    @Override
    public List<Document> parse(DocumentSource source) {
        try {
            String fileName = source.filename();
            long start = System.currentTimeMillis();

            String fileId = uploadFile(source);

            ParsingCreateResponse job = client.parsing().create(ParsingCreateParams.builder()
                    .fileId(fileId)
                    .tier(resolveTier(properties.getTier()))
                    .version(ParsingCreateParams.Version.LATEST)
                    .build());

            ParsingGetResponse result = awaitCompletion(job.id(), fileName);

            List<Document> documents = extractMarkdown(result, fileName);
            log.info("LlamaParse 解析完成：{}，产出 {} 个片段，耗时 {} ms",
                    fileName, documents.size(), System.currentTimeMillis() - start);
            return documents;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LlamaParse 解析被中断：" + source.filename(), e);
        } catch (LlamaCloudServiceException e) {
            throw new IllegalStateException(String.format(
                    "LlamaParse 云端返回错误（%s，HTTP %d）：%s",
                    source.filename(), e.statusCode(), e.body()), e);
        } catch (LlamaCloudIoException e) {
            throw new IllegalStateException(
                    "LlamaParse 网络异常（" + source.filename() + "）：" + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("LlamaParse 解析失败：" + source.filename(), e);
        }
    }

    /** 上传文件到 LlamaCloud，返回 fileId。 */
    private String uploadFile(DocumentSource source) throws Exception {
        FileCreateParams params = FileCreateParams.builder()
                .file(MultipartField.<InputStream>builder()
                        .value(source.inputStream())
                        .filename(source.filename())
                        .build())
                .purpose("parse")
                .build();
        FileCreateResponse uploaded = client.files().create(params);
        return uploaded.id();
    }

    /** 轮询任务直到终态；超时则尽力取消任务并抛出异常。 */
    private ParsingGetResponse awaitCompletion(String jobId, String fileName) throws InterruptedException {
        ParsingGetParams params = ParsingGetParams.builder()
                .jobId(jobId)
                .addExpand("markdown")
                .build();
        long deadline = System.currentTimeMillis() + properties.getTimeoutMinutes() * 60_000L;
        while (true) {
            ParsingGetResponse resp = client.parsing().get(params);
            String status = resp.job().status().value().name();
            switch (status) {
                case "COMPLETED" -> {
                    return resp;
                }
                case "FAILED", "CANCELLED" -> {
                    String err = resp.job().errorMessage().orElse("");
                    throw new IllegalStateException(
                            "LlamaParse 任务终态 " + status + "（jobId=" + jobId + "）：" + err);
                }
                default -> log.debug("LlamaParse 任务进行中（jobId={}，status={}）", jobId, status);
            }
            if (System.currentTimeMillis() > deadline) {
                cancelQuietly(jobId);
                throw new IllegalStateException(
                        "LlamaParse 解析超时（>" + properties.getTimeoutMinutes() + " 分钟）：" + fileName);
            }
            Thread.sleep(properties.getPollIntervalMs());
        }
    }

    /** 尽力取消任务，失败仅记录日志。 */
    private void cancelQuietly(String jobId) {
        try {
            client.parsing().cancel(jobId);
        } catch (Exception e) {
            log.warn("取消 LlamaParse 任务失败（jobId={}）：{}", jobId, e.getMessage());
        }
    }

    /** 提取按页 Markdown（每页一个 Document）；无分页结果时回退整文档 Markdown。 */
    private List<Document> extractMarkdown(ParsingGetResponse result, String fileName) {
        List<Document> documents = new ArrayList<>();
        var markdown = result.markdown();
        if (markdown.isPresent()) {
            for (var page : markdown.get().pages()) {
                if (page.isMarkdownResult()) {
                    var pageResult = page.asMarkdownResult();
                    // 归一化云端输出中可能残留的内嵌 HTML 片段（如 <table>）
                    String pageMd = htmlToMarkdownConverter.normalizeMarkdown(pageResult.markdown());
                    if (pageMd.isBlank()) continue;
                    Map<String, Object> metadata = baseMetadata(fileName);
                    metadata.put("page", pageResult.pageNumber());
                    documents.add(new Document(pageMd, metadata));
                } else if (page.isFailedMarkdown()) {
                    log.warn("LlamaParse 某页解析失败：{}", fileName);
                }
            }
        }
        if (documents.isEmpty()) {
            var full = result.markdownFull();
            if (full.isPresent() && !full.get().isBlank()) {
                documents.add(new Document(
                        htmlToMarkdownConverter.normalizeMarkdown(full.get()), baseMetadata(fileName)));
            }
        }
        return documents;
    }

    private Map<String, Object> baseMetadata(String fileName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_file", fileName);
        metadata.put("parser", "llamaparse");
        return metadata;
    }

    /** 配置字符串 → SDK Tier 常量（兼容 COST-EFFECTIVE / cost_effective 等写法）。 */
    private ParsingCreateParams.Tier resolveTier(String tier) {
        String normalized = tier == null ? "" : tier.trim().toUpperCase().replace("-", "_");
        return switch (normalized) {
            case "COST_EFFECTIVE" -> ParsingCreateParams.Tier.COST_EFFECTIVE;
            case "AGENTIC" -> ParsingCreateParams.Tier.AGENTIC;
            case "AGENTIC_PLUS" -> ParsingCreateParams.Tier.AGENTIC_PLUS;
            default -> ParsingCreateParams.Tier.FAST;
        };
    }
}
