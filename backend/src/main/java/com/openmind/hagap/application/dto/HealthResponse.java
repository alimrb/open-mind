package com.openmind.hagap.application.dto;

import java.util.Map;

public record HealthResponse(
        String status,
        Map<String, ComponentHealth> components
) {
    public record ComponentHealth(
            String status,
            String details
    ) {}
}
