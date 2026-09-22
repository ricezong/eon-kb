package cn.kong.kb.ingestion.polish;

/**
 * 基于时间间隔的简单速率限制器。
 * <p>
 * 将相邻请求的间隔强制为固定间隔（60s / maxRequestsPerMinute），
 * 即固定间隔节流而非滑动窗口——对"保护外部 LLM API 配额"的场景足够。
 * 作为 {@link ChunkPolishService} 的成员字段，Spring 单例天然全局共享，
 * 保证并发文档上传场景下的总请求速率。
 * </p>
 */
public class RateLimiter {

    private final long intervalMs;
    private long lastRequestTime = 0;

    /**
     * @param maxRequestsPerMinute 每分钟最大请求数
     */
    public RateLimiter(int maxRequestsPerMinute) {
        this.intervalMs = 60_000L / maxRequestsPerMinute;
    }

    /**
     * 阻塞等待直到可以发送下一个请求。
     *
     * @throws InterruptedException 如果等待期间被中断
     */
    public synchronized void acquire() throws InterruptedException {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        if (elapsed < intervalMs) {
            Thread.sleep(intervalMs - elapsed);
        }
        lastRequestTime = System.currentTimeMillis();
    }
}
