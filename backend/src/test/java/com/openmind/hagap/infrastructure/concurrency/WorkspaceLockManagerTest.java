package com.openmind.hagap.infrastructure.concurrency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceLockManagerTest {

    private WorkspaceLockManager lockManager;

    @BeforeEach
    void setUp() {
        lockManager = new WorkspaceLockManager();
    }

    @Test
    void shouldAcquireLock() {
        UUID workspaceId = UUID.randomUUID();

        assertThat(lockManager.tryAcquire(workspaceId)).isTrue();
        lockManager.release(workspaceId);
    }

    @Test
    void shouldAllowUpToThreeConcurrentAcquisitions() {
        UUID workspaceId = UUID.randomUUID();

        assertThat(lockManager.tryAcquire(workspaceId)).isTrue();
        assertThat(lockManager.tryAcquire(workspaceId)).isTrue();
        assertThat(lockManager.tryAcquire(workspaceId)).isTrue();

        assertThat(lockManager.availablePermits(workspaceId)).isZero();

        lockManager.release(workspaceId);
        lockManager.release(workspaceId);
        lockManager.release(workspaceId);
    }

    @Test
    void shouldReleaseAndAllowReacquisition() {
        UUID workspaceId = UUID.randomUUID();

        lockManager.tryAcquire(workspaceId);
        lockManager.tryAcquire(workspaceId);
        lockManager.tryAcquire(workspaceId);

        lockManager.release(workspaceId);
        assertThat(lockManager.availablePermits(workspaceId)).isEqualTo(1);

        assertThat(lockManager.tryAcquire(workspaceId)).isTrue();
        assertThat(lockManager.availablePermits(workspaceId)).isZero();

        lockManager.release(workspaceId);
        lockManager.release(workspaceId);
        lockManager.release(workspaceId);
    }

    @Test
    void shouldIsolateWorkspaces() {
        UUID workspace1 = UUID.randomUUID();
        UUID workspace2 = UUID.randomUUID();

        // Exhaust workspace1
        lockManager.tryAcquire(workspace1);
        lockManager.tryAcquire(workspace1);
        lockManager.tryAcquire(workspace1);

        // workspace2 should still be available
        assertThat(lockManager.tryAcquire(workspace2)).isTrue();
        assertThat(lockManager.availablePermits(workspace2)).isEqualTo(2);

        lockManager.release(workspace1);
        lockManager.release(workspace1);
        lockManager.release(workspace1);
        lockManager.release(workspace2);
    }

    @Test
    void shouldReturnMaxPermitsForUnknownWorkspace() {
        assertThat(lockManager.availablePermits(UUID.randomUUID())).isEqualTo(3);
    }

    @Test
    void shouldHandleConcurrentAccess() throws InterruptedException {
        UUID workspaceId = UUID.randomUUID();
        int threadCount = 10;
        AtomicInteger acquired = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    if (lockManager.tryAcquire(workspaceId)) {
                        acquired.incrementAndGet();
                        Thread.sleep(100); // hold the lock briefly
                        lockManager.release(workspaceId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // All 10 threads should eventually acquire since they release quickly
        assertThat(acquired.get()).isEqualTo(threadCount);
    }

    @Test
    void shouldHandleReleaseWithoutAcquire() {
        UUID workspaceId = UUID.randomUUID();

        // Release on non-existent semaphore should not throw
        lockManager.release(workspaceId);
    }
}
