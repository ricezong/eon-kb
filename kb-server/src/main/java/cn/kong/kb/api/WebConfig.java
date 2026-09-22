package cn.kong.kb.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web 配置类，用于配置 CORS 跨域及其他 MVC 相关设置。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 允许的跨域来源模式，逗号分隔。
     * 默认 "*"（本地开发可用）；
     * 生产部署建议通过 {@code app.cors.allowed-origin-patterns} 配置为显式域名列表，
     * 尤其当 allowCredentials=true 时——浏览器规范不允许 * 源携带凭证。
     */
    @Value("${app.cors.allowed-origin-patterns:*}")
    private List<String> allowedOriginPatterns;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
