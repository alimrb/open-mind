package com.openmind.hagap.application.service;

import com.openmind.hagap.application.dto.ChatRequest;
import com.openmind.hagap.application.dto.ChatResponse;
import com.openmind.hagap.application.dto.CitationDto;
import com.openmind.hagap.domain.model.*;
import com.openmind.hagap.infrastructure.cli.CliOutput;
import com.openmind.hagap.infrastructure.cli.OpenCodeCliExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final WorkspaceService workspaceService;
    private final SessionService sessionService;
    private final OpenCodeCliExecutor cliExecutor;
    private final RagService ragService;

    public ChatResponse chat(ChatRequest request) {
        if (request.useRag()) {
            return ragService.ragChat(request);
        }

        Workspace workspace = workspaceService.findWorkspace(request.workspaceId());
        Path workspacePath = Path.of(workspace.getDirectoryPath());

        Session session = sessionService.getOrCreateSession(
                request.sessionId(), workspace, extractTitle(request.message()));

        sessionService.saveMessage(session, workspace, MessageRole.USER,
                request.message(), null, null);

        CliOutput output = cliExecutor.execute(request.message(), workspacePath, null);

        String responseContent = output.getContent();
        List<CitationDto> citations = Collections.emptyList();
        Double confidence = null;

        Message assistantMessage = sessionService.saveMessage(
                session, workspace, MessageRole.ASSISTANT,
                responseContent, null, confidence);

        return new ChatResponse(
                assistantMessage.getId(),
                session.getId(),
                responseContent,
                confidence,
                citations,
                assistantMessage.getCreatedAt()
        );
    }

    private String extractTitle(String message) {
        if (message.length() <= 50) return message;
        return message.substring(0, 47) + "...";
    }
}
