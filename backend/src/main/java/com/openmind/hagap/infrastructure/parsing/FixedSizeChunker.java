package com.openmind.hagap.infrastructure.parsing;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FixedSizeChunker implements ChunkingStrategy {

    private static final int MAX_TOKENS = 512;
    private static final int OVERLAP_TOKENS = 50;
    private static final double CHARS_PER_TOKEN = 4.0;

    @Override
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int maxChars = (int) (MAX_TOKENS * CHARS_PER_TOKEN);
        int overlapChars = (int) (OVERLAP_TOKENS * CHARS_PER_TOKEN);

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());

            if (end < text.length()) {
                int lastNewline = text.lastIndexOf('\n', end);
                if (lastNewline > start + overlapChars) {
                    end = lastNewline + 1;
                }
            }

            chunks.add(text.substring(start, end).trim());

            if (end >= text.length()) break;

            int nextStart = end - overlapChars;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }

        return chunks.stream()
                .filter(c -> !c.isBlank())
                .toList();
    }

    public int estimateTokenCount(String text) {
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }
}
