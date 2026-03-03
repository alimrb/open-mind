package com.openmind.hagap.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Isolated thread pool for CLI process execution — deliberately NOT using Spring's default
 * {@code @Async} pool because one slow CLI call (up to 120s) would starve unrelated async tasks.
 *
 * <p>Sizing rationale (core=4, max=8, queue=16):
 * <ul>
 *   <li>Core 4: matches typical concurrent workspaces in dev/staging. Each CLI call is I/O-bound
 *       (waiting on process stdout), so cores &gt; CPU count is fine.</li>
 *   <li>Queue 16: small intentionally — backpressure is better than unbounded queueing.
 *       An unbounded queue ({@code LinkedBlockingQueue}) would hide overload until OOM.</li>
 *   <li>Rejection handler throws domain {@code CliExecutionException} instead of JDK's
 *       {@code RejectedExecutionException} — this maps cleanly to 503 in the GlobalExceptionHandler
 *       without needing a separate catch clause in every caller.</li>
 *   <li>Thread name prefix "cli-exec-" is critical for production debugging — makes these threads
 *       instantly identifiable in thread dumps and log MDC output.</li>
 * </ul>
 *
 * <p>Works in tandem with {@link WorkspaceLockManager}: this pool caps global concurrency,
 * the semaphore caps per-workspace concurrency. Two layers because a single workspace
 * shouldn't monopolize the entire pool.
 */
@Configuration
public class AsyncConfig {

    /**
     * Named bean — injected via {@code @Qualifier("cliExecutor")} in ChatController.
     * Explicit name avoids ambiguity if other executors are added later.
     */
    @Bean(name = "cliExecutor")
    public Executor cliTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("cli-exec-");
        executor.setRejectedExecutionHandler((r, e) -> {
            throw new com.openmind.hagap.domain.exception.CliExecutionException(
                    "CLI execution pool exhausted — too many concurrent requests");
        });
        executor.initialize();
        return executor;
    }
}
