package com.openmind.hagap.application.dto;

import com.openmind.hagap.domain.model.Message;
import com.openmind.hagap.domain.model.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID sessionId,
        MessageRole role,
        String content,
        String citationsJson,
        Double confidence,
        Instant createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSession().getId(),
                message.getRole(),
                message.getContent(),
                message.getCitationsJson(),
                message.getConfidence(),
                message.getCreatedAt()
        );
    }
}
