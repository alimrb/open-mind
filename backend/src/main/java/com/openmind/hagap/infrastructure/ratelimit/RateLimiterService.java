package com.openmind.hagap.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token bucket rate limiter (Bucket4j) — chosen over sliding window log because
 * token bucket is O(1) per request vs O(n) for window-based approaches. Greedy refill
 * (tokens restored continuously) avoids the burst-at-window-boundary problem of interval refill.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>In-memory over Redis-backed: acceptable for single-instance deployment. For horizontal
 *       scaling, would migrate to {@code Bucket4j-Redis} (same API, just swap the ProxyManager).</li>
 *   <li>Dual-key strategy (workspace ID + client IP): workspace key handles authenticated load,
 *       IP key catches unauthenticated abuse. Both share the same 30 req/min limit.</li>
 *   <li>{@link ConcurrentHashMap} grows unbounded per unique key — same trade-off as WorkspaceLockManager.
 *       Production mitigation: the rejection counter alerts on abuse patterns before map size matters.</li>
 *   <li>Micrometer counter on rejection enables Grafana alerting on rate limit spikes —
 *       this is the early warning for DDoS or misbehaving clients.</li>
 * </ul>
 */
@Service
@Slf4j
public class RateLimiterService {

    private static final int REQUESTS_PER_MINUTE = 30;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Counter rejectionCounter;

    public RateLimiterService(@Qualifier("rateLimitRejectionCounter") Counter rejectionCounter) {
        this.rejectionCounter = rejectionCounter;
    }

    /** Workspace-scoped rate check — used when the caller is authenticated and we know the workspace. */
    public boolean tryConsume(UUID workspaceId) {
        return tryConsume("workspace:" + workspaceId);
    }

    /** IP-scoped fallback — used by the interceptor before the request is authenticated. */
    public boolean tryConsumeByIp(String ipAddress) {
        return tryConsume("ip:" + ipAddress);
    }

    private boolean tryConsume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket());
        boolean consumed = bucket.tryConsume(1);
        if (!consumed) {
            rejectionCounter.increment();
            log.warn("Rate limit exceeded for key: {}", key);
        }
        return consumed;
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(REQUESTS_PER_MINUTE)
                .refillGreedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
