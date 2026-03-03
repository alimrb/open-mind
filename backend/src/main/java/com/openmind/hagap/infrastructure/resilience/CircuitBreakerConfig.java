package com.openmind.hagap.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Per-dependency circuit breakers (CLI, Qdrant, Embedding) — each tracks failures independently
 * so a Qdrant outage doesn't block direct CLI chat (non-RAG mode still works).
 *
 * <p>Configuration rationale:
 * <ul>
 *   <li>Sliding window of 10 (not count-based) — count-based resets on each window boundary
 *       causing blind spots. Sliding window provides continuous failure rate monitoring.</li>
 *   <li>50% threshold + minimum 5 calls — avoids tripping on 1-of-2 failures during low traffic.
 *       Too low a threshold causes flapping; too high defeats the purpose.</li>
 *   <li>30s open-state wait — long enough for transient issues (DNS, connection pool exhaustion)
 *       to resolve, short enough to recover quickly after a brief outage.</li>
 *   <li>Retry wraps the breaker (retry → circuit breaker → call), not the other way around.
 *       If the breaker is OPEN, retry attempts short-circuit immediately without wasting time.</li>
 *   <li>Exponential backoff (500ms → 1s → 2s) prevents thundering herd on recovery.</li>
 * </ul>
 */
@Configuration
public class CircuitBreakerConfig {

    /** Shared config — all breakers use the same thresholds. Per-breaker tuning not needed yet. */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        var config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public CircuitBreaker cliCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("cli");
    }

    @Bean
    public CircuitBreaker qdrantCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("qdrant");
    }

    @Bean
    public CircuitBreaker embeddingCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("embedding");
    }

    /** 3 attempts max — beyond that, it's not transient and the breaker should trip instead. */
    @Bean
    public RetryRegistry retryRegistry() {
        var config = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(500, 2))
                .build();

        return RetryRegistry.of(config);
    }

    @Bean
    public Retry qdrantRetry(RetryRegistry registry) {
        return registry.retry("qdrant");
    }
}
