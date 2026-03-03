package com.openmind.hagap.infrastructure.idempotency;

import com.openmind.hagap.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Test
    void shouldReturnEmptyForNewKey() {
        Optional<Map> result = idempotencyService.checkIdempotency("new-key-123", Map.class);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldStoreAndRetrieveResult() {
        String key = "test-key-" + System.nanoTime();
        Map<String, String> response = Map.of("answer", "hello world");
        Map<String, String> request = Map.of("message", "test");

        idempotencyService.storeResult(key, request, response);

        @SuppressWarnings("unchecked")
        Optional<Map> result = idempotencyService.checkIdempotency(key, Map.class);

        assertThat(result).isPresent();
        assertThat(result.get()).containsEntry("answer", "hello world");
    }

    @Test
    void shouldReturnEmptyForNullKey() {
        Optional<Map> result = idempotencyService.checkIdempotency(null, Map.class);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForBlankKey() {
        Optional<Map> result = idempotencyService.checkIdempotency("  ", Map.class);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotStoreForNullKey() {
        // Should not throw
        idempotencyService.storeResult(null, Map.of("a", "b"), Map.of("c", "d"));
    }
}
