package com.openmind.hagap.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatResponse(
        UUID messageId,
        UUID sessionId,
        String content,
        Double confidence,
        List<CitationDto> citations,
        Instant timestamp
) {}
