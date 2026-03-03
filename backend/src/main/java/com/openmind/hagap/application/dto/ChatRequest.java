package com.openmind.hagap.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChatRequest(
        @NotNull UUID workspaceId,
        UUID sessionId,
        @NotBlank String message,
        boolean useRag
) {}
