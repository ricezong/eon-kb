package cn.kong.kb;

import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 独立知识库服务的启动类。
 *
 * <p>kb-core 的所有能力（数据源、向量库、检索/摄入/工具等 Bean）通过其
 * {@code KnowledgeBaseConfiguration} 自动装配引入，本启动类只负责扫描
 * kb-server 自身的 Web/聊天/生成组件。</p>
 *
 * <p>排除的自动配置说明：</p>
 * <ul>
 *   <li>{@link DataSourceAutoConfiguration} / {@link MybatisAutoConfiguration}：
 *       改由 kb-core 的独立数据源 {@code kbDataSource} + {@code kbSqlSessionFactory} 接管。</li>
 *   <li>{@link PgVectorStoreAutoConfiguration}：改由 kb-core 绑定到独立数据源的
 *       {@code kbVectorStore} 接管。</li>
 *   <li>{@link OpenAiEmbeddingAutoConfiguration}：改由 kb-core 的百炼 DashScope
 *       Embedding Bean 接管（Chat 模型自动配置保留，用于查询改写/生成）。</li>
 * </ul>
 */
@SpringBootApplication(
        scanBasePackages = {
                "cn.kong.kb.chat",
                "cn.kong.kb.generation",
                "cn.kong.kb.api"
        },
        exclude = {
                DataSourceAutoConfiguration.class,
                MybatisAutoConfiguration.class,
                PgVectorStoreAutoConfiguration.class,
                OpenAiEmbeddingAutoConfiguration.class
        }
)
public class EonKbApplication {

    public static void main(String[] args) {
        SpringApplication.run(EonKbApplication.class, args);
    }
}
