package com.openmind.hagap.application.dto;

import com.openmind.hagap.domain.model.Session;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID workspaceId,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getWorkspace().getId(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
