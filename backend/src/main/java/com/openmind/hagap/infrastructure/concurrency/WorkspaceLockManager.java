package com.openmind.hagap.infrastructure.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Per-workspace concurrency guard using {@link Semaphore} (3 permits) — chosen over
 * {@link java.util.concurrent.locks.ReentrantLock} because we need bounded parallelism,
 * not mutual exclusion. A mutex would serialize all CLI calls per workspace unnecessarily.
 *
 * <p>Trade-offs and design notes:
 * <ul>
 *   <li>{@link ConcurrentHashMap#computeIfAbsent} guarantees atomic lazy init without synchronized blocks.
 *       The lambda is evaluated at most once per key even under contention (CHM's bin-level locking).</li>
 *   <li>The map grows unbounded — acceptable because workspace count is small (10s, not millions).
 *       For high cardinality, would need a {@code CacheBuilder.weakValues()} or periodic eviction.</li>
 *   <li>{@code tryAcquire} with timeout (30s) prevents indefinite blocking. Timeout returns false
 *       to the caller (503), which is better than queueing requests that will likely time out anyway.</li>
 *   <li>InterruptedException restores the interrupt flag before returning — critical for cooperative
 *       shutdown. Swallowing the interrupt without restoring it breaks thread pool shutdown semantics.</li>
 * </ul>
 */
@Component
@Slf4j
public class WorkspaceLockManager {

    private static final int MAX_CONCURRENT_PER_WORKSPACE = 3;
    private static final int ACQUIRE_TIMEOUT_SECONDS = 30;

    private final Map<UUID, Semaphore> workspaceSemaphores = new ConcurrentHashMap<>();

    /**
     * Blocks up to 30s for a permit. Returns false on timeout rather than throwing —
     * the caller converts this to a 503, which gives the client a clear retry signal.
     */
    public boolean tryAcquire(UUID workspaceId) {
        Semaphore semaphore = workspaceSemaphores.computeIfAbsent(
                workspaceId, k -> new Semaphore(MAX_CONCURRENT_PER_WORKSPACE));
        try {
            boolean acquired = semaphore.tryAcquire(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire workspace lock for {} after {}s",
                        workspaceId, ACQUIRE_TIMEOUT_SECONDS);
            }
            return acquired;
        } catch (InterruptedException e) {
            // MUST restore interrupt flag — swallowing it breaks ExecutorService.shutdownNow()
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release(UUID workspaceId) {
        Semaphore semaphore = workspaceSemaphores.get(workspaceId);
        if (semaphore != null) {
            semaphore.release();
        }
    }

    public int availablePermits(UUID workspaceId) {
        Semaphore semaphore = workspaceSemaphores.get(workspaceId);
        return semaphore != null ? semaphore.availablePermits() : MAX_CONCURRENT_PER_WORKSPACE;
    }
}
