package cn.kong.kb.domain;

import java.util.Locale;
import java.util.Map;

/**
 * 支持的文档类型。
 *
 * <p>扩展名映射在本枚举内唯一登记，白名单校验（{@link #isSupportedFileName}）
 * 与类型识别（{@link #fromFileName}）共享同一份登记表——
 * 新增类型只需添加映射，无需改动任何调用方。</p>
 */
public enum KbDocumentType {
    PDF, DOCX, PPTX, XLSX, TXT, MD, HTML;

    /** 扩展名 → 文档类型的唯一登记表 */
    private static final Map<String, KbDocumentType> EXTENSION_TYPES = Map.ofEntries(
            Map.entry(".pdf", PDF),
            Map.entry(".doc", DOCX), Map.entry(".docx", DOCX),
            Map.entry(".ppt", PPTX), Map.entry(".pptx", PPTX),
            Map.entry(".xls", XLSX), Map.entry(".xlsx", XLSX),
            Map.entry(".txt", TXT),
            Map.entry(".md", MD),
            Map.entry(".html", HTML), Map.entry(".htm", HTML)
    );

    /**
     * 根据文件扩展名判断文档类型。
     *
     * <p>未知扩展名回退为 TXT（fail-open：作为内部解析的兜底语义）。
     * 对外入口（如上传接口）应先用 {@link #isSupportedFileName} 做白名单拦截，
     * 避免把任意二进制文件当 TXT 解析。</p>
     */
    public static KbDocumentType fromFileName(String fileName) {
        KbDocumentType type = resolve(fileName);
        return type != null ? type : TXT;
    }

    /**
     * 扩展名是否在支持范围内（fail-closed：供上传入口做白名单校验）。
     */
    public static boolean isSupportedFileName(String fileName) {
        return resolve(fileName) != null;
    }

    private static KbDocumentType resolve(String fileName) {
        if (fileName == null) return null;
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, KbDocumentType> entry : EXTENSION_TYPES.entrySet()) {
            if (lower.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
