package com.openmind.hagap.application.service;

import com.openmind.hagap.infrastructure.vector.VectorSearchResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HallucinationControlService {

    private static final double CONFIDENCE_THRESHOLD = 0.75;

    public double calculateConfidence(List<VectorSearchResult> searchResults) {
        if (searchResults.isEmpty()) {
            return 0.0;
        }

        double avgScore = searchResults.stream()
                .mapToDouble(VectorSearchResult::getScore)
                .average()
                .orElse(0.0);

        double topScore = searchResults.stream()
                .mapToDouble(VectorSearchResult::getScore)
                .max()
                .orElse(0.0);

        return (avgScore * 0.4) + (topScore * 0.6);
    }

    public boolean meetsThreshold(double confidence) {
        return confidence >= CONFIDENCE_THRESHOLD;
    }

    public double getThreshold() {
        return CONFIDENCE_THRESHOLD;
    }
}
