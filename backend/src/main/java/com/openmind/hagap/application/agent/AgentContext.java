package com.openmind.hagap.application.agent;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AgentContext {

    private final UUID workspaceId;
    private final UUID sessionId;
    private final Path workspacePath;
    private final List<Path> knowledgeFiles;
    private final String correlationId;
    private final String cliSessionId;
}
