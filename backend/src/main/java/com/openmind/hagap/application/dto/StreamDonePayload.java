package com.openmind.hagap.application.dto;

import java.util.List;
import java.util.UUID;

public record StreamDonePayload(
        UUID sessionId,
        UUID messageId,
        Double confidence,
        List<CitationDto> citations,
        List<McpComponentDto> mcpComponents,
        List<AgentStepDto> agentSteps
) {}
