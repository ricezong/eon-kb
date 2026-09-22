package cn.kong.kb.config;

import ai.llamaindex.llamacloud.client.LlamaCloudClient;
import ai.llamaindex.llamacloud.client.okhttp.LlamaCloudOkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LlamaParse（LlamaCloud）客户端装配。
 *
 * <p>仅当 {@code app.parsing.llamaparse.enabled=true} 时创建 {@link LlamaCloudClient}，
 * 未开启时项目不依赖任何外部解析服务，客户端 Bean 与云端解析器均不装配。</p>
 */
@Configuration
@EnableConfigurationProperties(LlamaParseProperties.class)
public class LlamaParseConfig {

    @Bean
    @ConditionalOnProperty(name = "app.parsing.llamaparse.enabled", havingValue = "true")
    public LlamaCloudClient llamaCloudClient(LlamaParseProperties properties) {
        return LlamaCloudOkHttpClient.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
