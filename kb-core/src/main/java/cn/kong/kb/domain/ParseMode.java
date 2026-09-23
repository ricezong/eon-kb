package cn.kong.kb.domain;

/**
 * 文档解析方式（由上传方选择，随文档一并持久化）。
 *
 * <p>三档语义互不重叠：</p>
 * <ul>
 *   <li>{@link #AUTO}  —— 按后端配置智能路由：{@code app.parsing.llamaparse.types}
 *       白名单内的类型（默认 PDF/DOCX/PPTX）在 LlamaParse 启用时走云端，其余本地。即历史默认行为。</li>
 *   <li>{@link #LOCAL} —— 强制本地解析（Tika / POI / jsoup），永不走云端。</li>
 *   <li>{@link #CLOUD} —— 强制云端 LlamaParse，<b>无视白名单</b>（任意类型都尝试云端）；
 *       仅当 LlamaParse 未装配或云端运行失败/返回空时，静默回退到该类型的本地解析器。</li>
 * </ul>
 */
public enum ParseMode {
    AUTO,
    LOCAL,
    CLOUD
}
