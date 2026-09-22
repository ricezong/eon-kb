package cn.kong.kb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池集中配置。
 *
 * <p>按职责拆分为两个线程池，互不影响：</p>
 * <ul>
 *   <li>{@code ingestionExecutor} — 文档摄入流水线（解析→分块→向量化→存储），CPU+IO 混合，单任务耗时长</li>
 *   <li>{@code searchExecutor} — 检索聊天（三路并行检索 + SSE 流式推送），IO 密集，单任务耗时短</li>
 * </ul>
 *
 * <p>所有参数均可通过 {@code application.yml} 的 {@code app.thread-pool.*} 配置，
 * 无需重新编译即可调优。</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 文档摄入线程池：解析→分块→向量化→存储（CPU+IO 混合，耗时长）。
     *
     * <p>核心线程 2、最大线程 4、队列容量 10。
     * 拒绝策略为 {@link ThreadPoolExecutor.CallerRunsPolicy}，
     * 队列满时由调用线程执行，起到背压保护作用。</p>
     */
    @Bean("ingestionExecutor")
    public Executor ingestionExecutor(
            @Value("${app.thread-pool.ingestion.core:2}") int core,
            @Value("${app.thread-pool.ingestion.max:4}") int max,
            @Value("${app.thread-pool.ingestion.queue:10}") int queue) {
        return build("ingestion-", core, max, queue);
    }

    /**
     * 检索聊天线程池：混合检索三路并行 + SSE 流式推送（IO 密集，耗时短）。
     *
     * <p>核心线程 4、最大线程 8、队列容量 50。
     * 拒绝策略为 {@link ThreadPoolExecutor.CallerRunsPolicy}。</p>
     */
    @Bean("searchExecutor")
    public Executor searchExecutor(
            @Value("${app.thread-pool.search.core:4}") int core,
            @Value("${app.thread-pool.search.max:8}") int max,
            @Value("${app.thread-pool.search.queue:50}") int queue) {
        return build("search-", core, max, queue);
    }

    /**
     * 构建通用 {@link ThreadPoolTaskExecutor}。
     *
     * @param prefix 线程名前缀，用于日志排查
     * @param core   核心线程数
     * @param max    最大线程数
     * @param queue  队列容量
     * @return 配置好的线程池
     */
    private ThreadPoolTaskExecutor build(String prefix, int core, int max, int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix(prefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭：等待正在执行的任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
