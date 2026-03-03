package com.openmind.hagap.infrastructure.ratelimit;

import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimiterServiceTest {

    private Counter rejectionCounter;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rejectionCounter = mock(Counter.class);
        rateLimiterService = new RateLimiterService(rejectionCounter);
    }

    @Test
    void shouldAllowRequestsWithinLimit() {
        UUID workspaceId = UUID.randomUUID();

        for (int i = 0; i < 30; i++) {
            assertThat(rateLimiterService.tryConsume(workspaceId)).isTrue();
        }

        verifyNoInteractions(rejectionCounter);
    }

    @Test
    void shouldRejectWhenRateLimitExceeded() {
        UUID workspaceId = UUID.randomUUID();

        // Consume all 30 tokens
        for (int i = 0; i < 30; i++) {
            rateLimiterService.tryConsume(workspaceId);
        }

        // 31st should be rejected
        assertThat(rateLimiterService.tryConsume(workspaceId)).isFalse();
        verify(rejectionCounter).increment();
    }

    @Test
    void shouldTrackRateLimitPerWorkspace() {
        UUID workspace1 = UUID.randomUUID();
        UUID workspace2 = UUID.randomUUID();

        // Exhaust workspace1
        for (int i = 0; i < 30; i++) {
            rateLimiterService.tryConsume(workspace1);
        }

        // workspace2 should still have capacity
        assertThat(rateLimiterService.tryConsume(workspace2)).isTrue();
    }

    @Test
    void shouldTrackByIpAddress() {
        String ip = "192.168.1.100";

        for (int i = 0; i < 30; i++) {
            assertThat(rateLimiterService.tryConsumeByIp(ip)).isTrue();
        }

        assertThat(rateLimiterService.tryConsumeByIp(ip)).isFalse();
        verify(rejectionCounter).increment();
    }

    @Test
    void shouldIsolateWorkspaceAndIpBuckets() {
        UUID workspaceId = UUID.randomUUID();

        // Exhaust workspace bucket
        for (int i = 0; i < 30; i++) {
            rateLimiterService.tryConsume(workspaceId);
        }

        // IP bucket for same identifier should be independent
        assertThat(rateLimiterService.tryConsumeByIp("10.0.0.1")).isTrue();
    }
}
