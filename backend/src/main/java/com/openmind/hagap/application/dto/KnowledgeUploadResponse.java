package com.openmind.hagap.application.dto;

import java.util.UUID;

public record KnowledgeUploadResponse(
        UUID fileId,
        String filename,
        String status,
        int chunkCount
) {}
