package cn.kong.kb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 百炼 Rerank 接口客户端配置。
 */
@Configuration
public class RerankConfig {

    @Value("${app.rerank.base-url}")
    private String rerankBaseUrl;

    @Value("${app.rerank.api-key}")
    private String rerankApiKey;

    @Bean("rerankRestClient")
    public RestClient rerankRestClient() {
        return RestClient.builder()
                .baseUrl(rerankBaseUrl)
                .defaultHeader("Authorization", "Bearer " + rerankApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
