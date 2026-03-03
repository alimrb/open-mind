package com.openmind.hagap.application.service;

import com.openmind.hagap.infrastructure.vector.VectorSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HallucinationControlServiceTest {

    private HallucinationControlService service;

    @BeforeEach
    void setUp() {
        service = new HallucinationControlService();
    }

    @Test
    void shouldReturnZeroConfidenceForEmptyResults() {
        double confidence = service.calculateConfidence(Collections.emptyList());
        assertEquals(0.0, confidence);
    }

    @Test
    void shouldCalculateConfidenceFromScores() {
        List<VectorSearchResult> results = List.of(
                VectorSearchResult.builder().chunkId(UUID.randomUUID())
                        .content("chunk1").sourceFile("file.md")
                        .chunkIndex(0).score(0.9).build(),
                VectorSearchResult.builder().chunkId(UUID.randomUUID())
                        .content("chunk2").sourceFile("file.md")
                        .chunkIndex(1).score(0.8).build()
        );

        double confidence = service.calculateConfidence(results);

        // avgScore = 0.85, topScore = 0.9
        // confidence = 0.85 * 0.4 + 0.9 * 0.6 = 0.34 + 0.54 = 0.88
        assertTrue(confidence > 0.85);
        assertTrue(confidence < 0.95);
    }

    @Test
    void shouldMeetThresholdForHighConfidence() {
        assertTrue(service.meetsThreshold(0.8));
        assertTrue(service.meetsThreshold(0.75));
    }

    @Test
    void shouldNotMeetThresholdForLowConfidence() {
        assertFalse(service.meetsThreshold(0.5));
        assertFalse(service.meetsThreshold(0.74));
    }

    @Test
    void shouldReturnCorrectThreshold() {
        assertEquals(0.75, service.getThreshold());
    }

    @Test
    void shouldHandleSingleResult() {
        List<VectorSearchResult> results = List.of(
                VectorSearchResult.builder().chunkId(UUID.randomUUID())
                        .content("chunk").sourceFile("file.md")
                        .chunkIndex(0).score(0.6).build()
        );

        double confidence = service.calculateConfidence(results);
        // avgScore = 0.6, topScore = 0.6
        // confidence = 0.6 * 0.4 + 0.6 * 0.6 = 0.24 + 0.36 = 0.60
        assertEquals(0.6, confidence, 0.01);
        assertFalse(service.meetsThreshold(confidence));
    }
}
