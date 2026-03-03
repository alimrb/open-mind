package com.openmind.hagap.domain.exception;

public class InsufficientEvidenceException extends RuntimeException {

    private final double confidence;

    public InsufficientEvidenceException(double confidence) {
        super("Insufficient evidence to answer confidently. Confidence: " + confidence);
        this.confidence = confidence;
    }

    public double getConfidence() {
        return confidence;
    }
}
