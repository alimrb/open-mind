package com.openmind.hagap.infrastructure.websocket;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class SystemEvent {

    public enum EventType {
        INDEXING_PROGRESS,
        WORKFLOW_STEP_STARTED,
        WORKFLOW_STEP_FINISHED,
        SYSTEM_ALERT,
        COLLECTOR_STATUS
    }

    private final EventType type;
    private final UUID workspaceId;
    private final String message;
    private final Object payload;
    @Builder.Default
    private final Instant timestamp = Instant.now();
}
