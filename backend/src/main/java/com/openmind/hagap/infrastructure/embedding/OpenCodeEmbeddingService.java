package com.openmind.hagap.infrastructure.embedding;

import com.openmind.hagap.infrastructure.config.OpenCodeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenCodeEmbeddingService implements EmbeddingService {

    private final OpenCodeProperties properties;

    @Override
    public List<Float> generateEmbedding(String text) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    properties.getBinaryPath(), "embed", "--format", "json", text
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining());
            }

            process.waitFor();

            return parseEmbeddingOutput(output);

        } catch (Exception e) {
            log.warn("Embedding generation failed, returning zero vector", e);
            return generateZeroVector();
        }
    }

    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        return texts.stream()
                .map(this::generateEmbedding)
                .toList();
    }

    private List<Float> parseEmbeddingOutput(String output) {
        try {
            String cleaned = output.replaceAll("[\\[\\]]", "").trim();
            if (cleaned.isEmpty()) return generateZeroVector();

            return Arrays.stream(cleaned.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Float::parseFloat)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse embedding output", e);
            return generateZeroVector();
        }
    }

    private List<Float> generateZeroVector() {
        List<Float> vector = new ArrayList<>(768);
        for (int i = 0; i < 768; i++) {
            vector.add(0.0f);
        }
        return vector;
    }
}
