package com.openmind.hagap.infrastructure.vector;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class VectorSearchResult {

    private final UUID chunkId;
    private final String content;
    private final String sourceFile;
    private final int chunkIndex;
    private final double score;
}
