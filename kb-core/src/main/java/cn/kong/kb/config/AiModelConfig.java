package cn.kong.kb.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 向量模型配置。
 */
@Configuration
public class AiModelConfig {

    @Value("${app.embedding.api-key}")
    private String embeddingApiKey;

    @Value("${app.embedding.base-url}")
    private String embeddingBaseUrl;

    @Value("${app.embedding.model}")
    private String embeddingModel;

    @Value("${app.embedding.dimensions}")
    private int embeddingDimensions;

    /**
     * 自定义 EmbeddingModel Bean，指向百炼 DashScope 平台。
     * Spring AI 自动配置的 OpenAI EmbeddingModel（若有）会被此 Bean 覆盖。
     */
    @Bean
    @Primary
    public EmbeddingModel dashScopeEmbeddingModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(embeddingApiKey)
                .baseUrl(embeddingBaseUrl)
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModel)
                .dimensions(embeddingDimensions)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }
}
