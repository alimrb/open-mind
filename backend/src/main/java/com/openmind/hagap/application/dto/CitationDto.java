package com.openmind.hagap.application.dto;

import java.util.UUID;

public record CitationDto(
        UUID chunkId,
        String sourceFile,
        int chunkIndex,
        String snippet,
        double score
) {}
