package cn.kong.kb.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 知识库独立数据源配置。
 *
 * <p>本模块自带一套 <b>独立</b> 的持久化基础设施，与宿主应用（agent）自身的
 * 数据源完全隔离，互不干扰：</p>
 * <ul>
 *   <li>{@code kbDataSource}       — 绑定 {@code app.datasource.*}，非 {@code @Primary}，
 *                                     宿主仍保留自己的主数据源。</li>
 *   <li>{@code kbJdbcTemplate}     — 供 {@link PgVectorStore} 使用。</li>
 *   <li>{@code kbSqlSessionFactory}— 加载 kb-core 内的 MyBatis Mapper XML / 类型别名 / 类型处理器，
 *                                     并开启下划线转驼峰。</li>
 *   <li>{@code kbVectorStore}      — 绑定到 {@code kbDataSource} 的 PgVectorStore，
 *                                     标记 {@code @Primary} 以便注入点唯一解析。</li>
 *   <li>{@code kbTransactionManager} — 知识库侧事务管理器，供"先删后插"这类
 *                                     多语句原子操作使用（如文档-知识库关联的重建）。</li>
 * </ul>
 *
 * <p>{@code @MapperScan} 通过 {@code sqlSessionFactoryRef} 将
 * {@code cn.kong.kb.mapper} 下的 Mapper 明确绑定到本模块的会话工厂，
 * 不会与宿主可能存在的其它 {@code SqlSessionFactory} 冲突。</p>
 *
 * <p><b>宿主须知：</b>若宿主应用自身也使用 MyBatis 且依赖其自动配置，
 * 请将宿主自己的 {@code SqlSessionFactory} 标记为 {@code @Primary}，
 * 以避免按类型注入时出现歧义（KB 侧已通过 {@code sqlSessionFactoryRef} 显式绑定，不受影响）。</p>
 */
@Configuration
@MapperScan(basePackages = "cn.kong.kb.mapper",
        sqlSessionFactoryRef = "kbSqlSessionFactory")
public class KbDataSourceConfig {

    @Value("${spring.ai.vectorstore.pgvector.table-name:kb_chunks}")
    private String vectorTableName;

    @Value("${spring.ai.vectorstore.pgvector.dimensions:1024}")
    private int vectorDimensions;

    @Value("${spring.ai.vectorstore.pgvector.max-document-batch-size:20}")
    private int maxDocumentBatchSize;

    /**
     * 知识库专属数据源。属性前缀 {@code app.datasource.*}
     * （url / username / password / driver-class-name）。
     * 非 {@code @Primary}，宿主的主数据源不受影响。
     */
    @Bean(name = "kbDataSource")
    @ConfigurationProperties(prefix = "app.datasource")
    public DataSource kbDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 绑定到 {@code kbDataSource} 的 JdbcTemplate，供 PgVectorStore 使用。
     */
    @Bean(name = "kbJdbcTemplate")
    public JdbcTemplate kbJdbcTemplate(@Qualifier("kbDataSource") DataSource kbDataSource) {
        return new JdbcTemplate(kbDataSource);
    }

    /**
     * 知识库专属 MyBatis 会话工厂：加载 kb-core jar 内的 Mapper XML，
     * 注册领域别名与类型处理器，开启下划线转驼峰。
     */
    @Bean(name = "kbSqlSessionFactory")
    public SqlSessionFactory kbSqlSessionFactory(
            @Qualifier("kbDataSource") DataSource kbDataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(kbDataSource);
        // classpath*: 确保能从 kb-core 依赖 jar 中解析到 XML
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/*.xml"));
        factoryBean.setTypeAliasesPackage("cn.kong.kb.domain");
        factoryBean.setTypeHandlersPackage("cn.kong.kb.config");

        org.apache.ibatis.session.Configuration configuration =
                new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);

        return factoryBean.getObject();
    }

    /**
     * 知识库侧事务管理器：绑定 {@code kbDataSource}，
     * 供文档-知识库关联等"多语句原子操作"使用（如先删后插的重建）。
     * Mapper 会话工厂与 JdbcTemplate 均使用同一数据源，
     * 会自动加入本事务管理器开启的事务。
     */
    @Bean
    public DataSourceTransactionManager kbTransactionManager(
            @Qualifier("kbDataSource") DataSource kbDataSource) {
        return new DataSourceTransactionManager(kbDataSource);
    }

    /**
     * 绑定到独立数据源的向量库。标记 {@code @Primary}，
     * 使 {@code IngestionPipeline} 等注入点唯一解析到本 Bean；
     * 同时让宿主的 PgVectorStore 自动配置（若为 {@code @ConditionalOnMissingBean}）退避。
     */
    @Bean(name = "kbVectorStore")
    @Primary
    public PgVectorStore kbVectorStore(@Qualifier("kbJdbcTemplate") JdbcTemplate kbJdbcTemplate,
                                       EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(kbJdbcTemplate, embeddingModel)
                .dimensions(vectorDimensions)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .vectorTableName(vectorTableName)
                .initializeSchema(false)
                .maxDocumentBatchSize(maxDocumentBatchSize)
                .build();
    }
}
