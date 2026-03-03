package com.openmind.hagap.application.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String description,
        String directoryPath,
        Instant createdAt
) {}
