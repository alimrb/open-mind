package com.openmind.hagap.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics following the RED method (Rate, Errors, Duration) for each external dependency.
 *
 * <p>Timers (duration/rate) for the three I/O-bound operations: CLI execution, embedding generation,
 * and vector search. Counters (error signals) for hallucination rejections, idempotency cache hits,
 * and rate limit rejections. Together these cover the three questions you always ask in an outage:
 * "How fast?" (timers), "How often does it fail?" (counters), "Is it getting worse?" (rate of change).
 *
 * <p>Named beans with {@code @Qualifier} injection — chose this over annotation-based metrics
 * ({@code @Timed}, {@code @Counted}) because: (1) Micrometer annotations require an AspectJ
 * proxy which adds complexity, (2) explicit Timer/Counter injection makes the metric recording
 * visible in the code — no hidden magic, easier to grep for "who records this metric?".
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Timer cliExecutionTimer(MeterRegistry registry) {
        return Timer.builder("hagap.cli.execution.duration")
                .description("CLI execution duration")
                .register(registry);
    }

    @Bean
    public Timer embeddingTimer(MeterRegistry registry) {
        return Timer.builder("hagap.embedding.duration")
                .description("Embedding generation duration")
                .register(registry);
    }

    @Bean
    public Timer vectorSearchTimer(MeterRegistry registry) {
        return Timer.builder("hagap.vector.search.duration")
                .description("Vector search duration")
                .register(registry);
    }

    @Bean
    public Counter hallucinationRejectionCounter(MeterRegistry registry) {
        return Counter.builder("hagap.hallucination.rejections")
                .description("Number of answers rejected due to insufficient evidence")
                .register(registry);
    }

    @Bean
    public Counter idempotencyHitCounter(MeterRegistry registry) {
        return Counter.builder("hagap.idempotency.hits")
                .description("Number of idempotent request cache hits")
                .register(registry);
    }

    @Bean
    public Counter rateLimitRejectionCounter(MeterRegistry registry) {
        return Counter.builder("hagap.ratelimit.rejections")
                .description("Number of requests rejected by rate limiter")
                .register(registry);
    }
}
