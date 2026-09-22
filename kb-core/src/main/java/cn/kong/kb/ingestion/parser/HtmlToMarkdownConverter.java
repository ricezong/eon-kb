package cn.kong.kb.ingestion.parser;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * HTML → Markdown 转换组件（基于 flexmark-html2md-converter）。
 *
 * <p>两层职责：</p>
 * <ol>
 *   <li>{@link #convert(String)} / {@link #convert(org.jsoup.nodes.Document)}：
 *       完整 HTML → Markdown。转换前先用 jsoup 移除 script/style 等不可见或
 *       有安全风险的节点，再做结构转换。</li>
 *   <li>{@link #normalizeMarkdown(String)}：对"已含 Markdown 的文本"做内嵌 HTML
 *       片段归一化（如云端解析输出中残留的 {@code <table>} 片段）——仅当围栏
 *       代码块之外检测到块级 HTML 标签时才过一遍转换，其余内容原样返回。</li>
 * </ol>
 */
@Component
public class HtmlToMarkdownConverter {

    /** 需要做片段归一化的块级 HTML 标签 */
    private static final Pattern BLOCK_HTML_TAG = Pattern.compile(
            "<(table|thead|tbody|tr|th|td|ul|ol|li|dl|dt|dd|h[1-6]|blockquote|pre)\\b",
            Pattern.CASE_INSENSITIVE);

    private final FlexmarkHtmlConverter converter;

    public HtmlToMarkdownConverter() {
        MutableDataSet options = new MutableDataSet();
        // 注册 GFM 扩展：<table> → MD 表格、<del> → ~~删除线~~、checkbox → - [ ]
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create()));
        this.converter = FlexmarkHtmlConverter.builder(options).build();
    }

    /**
     * 完整 HTML 字符串 → Markdown。
     */
    public String convert(String html) {
        if (html == null || html.isBlank()) return "";
        return convert(Jsoup.parse(html));
    }

    /**
     * 已由调用方解析好的 jsoup DOM → Markdown（避免重复解析）。
     */
    public String convert(org.jsoup.nodes.Document dom) {
        if (dom == null || dom.body() == null) return "";
        // 移除对 Markdown 输出无意义且有安全风险的节点
        dom.select("script, style, noscript, iframe, object, embed, form, input, button, svg").remove();
        String markdown = converter.convert(dom.body().html());
        return markdown == null ? "" : markdown.strip();
    }

    /**
     * 判断 Markdown 文本（围栏代码块之外）是否内嵌了块级 HTML 片段。
     */
    public boolean containsBlockHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) return false;
        // 按 ``` 切分：偶数下标段在围栏外，奇数下标段在代码块内（跳过，避免误判代码示例）
        String[] segments = markdown.split("```");
        for (int i = 0; i < segments.length; i += 2) {
            if (BLOCK_HTML_TAG.matcher(segments[i]).find()) return true;
        }
        return false;
    }

    /**
     * 归一化含内嵌 HTML 片段的 Markdown（如 LlamaParse 输出的 {@code <table>} 片段）。
     * 纯 Markdown 内容原样返回；归一化失败也不影响主流程。
     */
    public String normalizeMarkdown(String markdown) {
        if (markdown == null) return "";
        if (markdown.isBlank() || !containsBlockHtml(markdown)) return markdown;
        try {
            return convert(markdown);
        } catch (Exception e) {
            return markdown;
        }
    }
}
