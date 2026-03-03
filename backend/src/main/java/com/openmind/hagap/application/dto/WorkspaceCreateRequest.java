package com.openmind.hagap.application.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceCreateRequest(
        @NotBlank String name,
        String description
) {}
