package cn.kong.kb.config;

import cn.kong.kb.domain.KbDocumentType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumSet;
import java.util.Set;

/**
 * LlamaParse 云端解析配置（前缀 {@code app.parsing.llamaparse}）。
 *
 * <p>默认关闭（{@code enabled=false}），项目可纯本地运行；
 * 开启后仅对 {@link #types} 配置的文档类型走云端解析，其余类型始终本地解析。</p>
 */
@ConfigurationProperties(prefix = "app.parsing.llamaparse")
public class LlamaParseProperties {

    /** 云端解析总开关 */
    private boolean enabled = false;

    /** LlamaCloud API Key（llx- 开头） */
    private String apiKey = "";

    /** 服务地址 */
    private String baseUrl = "https://api.llamaindex.ai";

    /** 解析档位：FAST / COST_EFFECTIVE / AGENTIC / AGENTIC_PLUS（按页计费依次递增） */
    private String tier = "FAST";

    /** 任务轮询间隔（毫秒） */
    private long pollIntervalMs = 2000;

    /** 单文档解析总超时（分钟） */
    private int timeoutMinutes = 10;

    /** 允许走云端解析的文档类型；xlsx/txt/md/html 建议保持本地解析以节省配额 */
    private Set<KbDocumentType> types = EnumSet.of(
            KbDocumentType.PDF, KbDocumentType.DOCX, KbDocumentType.PPTX);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public Set<KbDocumentType> getTypes() {
        return types;
    }

    public void setTypes(Set<KbDocumentType> types) {
        this.types = types;
    }
}
