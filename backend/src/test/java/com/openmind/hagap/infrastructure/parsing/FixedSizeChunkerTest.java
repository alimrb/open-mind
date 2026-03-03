package com.openmind.hagap.infrastructure.parsing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FixedSizeChunkerTest {

    private FixedSizeChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new FixedSizeChunker();
    }

    @Test
    void shouldReturnEmptyForNullInput() {
        List<String> chunks = chunker.chunk(null);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void shouldReturnEmptyForBlankInput() {
        List<String> chunks = chunker.chunk("   ");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void shouldReturnSingleChunkForShortText() {
        String text = "This is a short text.";
        List<String> chunks = chunker.chunk(text);

        assertEquals(1, chunks.size());
        assertEquals("This is a short text.", chunks.get(0));
    }

    @Test
    void shouldSplitLongTextIntoMultipleChunks() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Word ").append(i).append(" ");
            if (i % 20 == 0) sb.append("\n");
        }
        String text = sb.toString();

        List<String> chunks = chunker.chunk(text);

        assertTrue(chunks.size() > 1, "Should produce multiple chunks");
        for (String chunk : chunks) {
            assertFalse(chunk.isBlank(), "Chunks should not be blank");
        }
    }

    @Test
    void shouldEstimateTokenCount() {
        String text = "Hello world test";
        int tokens = chunker.estimateTokenCount(text);

        assertTrue(tokens > 0);
        assertTrue(tokens <= text.length()); // 4 chars per token
    }

    @Test
    void shouldHandleTextWithNewlines() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("Line ").append(i).append("\n");
        }

        List<String> chunks = chunker.chunk(sb.toString());
        assertTrue(chunks.size() >= 1);
    }

    @Test
    void shouldNotProduceBlankChunks() {
        String text = "Some text\n\n\n\nMore text\n\n\n";
        List<String> chunks = chunker.chunk(text);

        for (String chunk : chunks) {
            assertFalse(chunk.isBlank());
        }
    }
}
