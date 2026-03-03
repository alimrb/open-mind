package com.openmind.hagap.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine over Guava Cache (deprecated) and EhCache (heavyweight, XML config).
 * Caffeine uses a W-TinyLFU eviction policy — near-optimal hit rate with O(1) operations,
 * significantly better than LRU under real workloads (see Caffeine benchmarks).
 *
 * <p>All three caches share one config (500 entries, 10 min TTL) — deliberately simple.
 * Per-cache tuning (e.g., embeddings could use longer TTL since vectors are deterministic)
 * is a premature optimization until cache miss metrics show a problem.
 * {@code recordStats()} exposes hit/miss/eviction counts to Micrometer → Prometheus → Grafana.
 * Without this, you're flying blind on whether the cache is actually helping.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String WORKSPACE_CACHE = "workspaces";
    public static final String EMBEDDING_CACHE = "embeddings";
    public static final String RAG_RESULTS_CACHE = "ragResults";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                WORKSPACE_CACHE, EMBEDDING_CACHE, RAG_RESULTS_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());
        return manager;
    }
}
