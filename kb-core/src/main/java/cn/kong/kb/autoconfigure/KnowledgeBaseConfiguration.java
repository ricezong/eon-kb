package cn.kong.kb.autoconfigure;

import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * kb-core 的自动装配入口。
 *
 * <p>宿主应用（agent）只要把 kb-core 作为依赖加入 classpath，本类即通过
 * {@code META-INF/spring/...AutoConfiguration.imports} 被 Spring Boot 自动加载，
 * 进而扫描并注册知识库所需的全部 Bean：独立数据源、向量库、检索/重排/摄入服务、
 * 以及对外工具 {@code DefaultKnowledgeBaseTool}。</p>
 *
 * <p>也可在宿主中显式 {@code @Import(KnowledgeBaseConfiguration.class)} 启用。</p>
 *
 * <p>{@code before} 排序确保本模块的 {@code kbVectorStore} / DashScope Embedding
 * 先于 Spring AI 的对应自动配置注册，使后者（{@code @ConditionalOnMissingBean}）退避，
 * 从而始终绑定到 KB 的独立数据源。</p>
 *
 * <p>注意：这里只扫描 kb-core 自身所在的包，<b>不</b>扫描 kb-server 的
 * {@code chat/generation/api} 包（那些类不在本 jar 中）。</p>
 */
@AutoConfiguration(before = {
        PgVectorStoreAutoConfiguration.class,
        OpenAiEmbeddingAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "cn.kong.kb.config",
        "cn.kong.kb.retrieval",
        "cn.kong.kb.rerank",
        "cn.kong.kb.ingestion",
        "cn.kong.kb.tool"
})
public class KnowledgeBaseConfiguration {
}
