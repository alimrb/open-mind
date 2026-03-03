package com.openmind.hagap.application.service;

import com.openmind.hagap.application.dto.ChatStreamRequest;
import com.openmind.hagap.application.dto.StreamEvent;
import com.openmind.hagap.domain.model.*;
import com.openmind.hagap.infrastructure.cli.OpenCodeCliExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Path;

/**
 * SSE streaming bridge between the CLI process and the HTTP client.
 *
 * <p>Production note: the initial empty SSE event (step_start with empty content) is intentional —
 * it forces HTTP response headers through any buffering reverse proxy (Nginx, Caddy, Docker
 * port-forward). Without it, the client sees 0 bytes until the proxy's response buffer fills
 * (~4KB) or the connection closes, causing events to arrive in bursts instead of streaming.
 * This works in tandem with Caddy's {@code flush_interval -1} for the SSE endpoint.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatStreamService {

    private final WorkspaceService workspaceService;
    private final SessionService sessionService;
    private final OpenCodeCliExecutor cliExecutor;

    @Async
    public void streamChat(ChatStreamRequest request, SseEmitter emitter) {
        try {
            Workspace workspace = workspaceService.findWorkspace(request.workspaceId());
            Path workspacePath = Path.of(workspace.getDirectoryPath());

            Session session = sessionService.getOrCreateSession(
                    request.sessionId(), workspace, extractTitle(request.message()));

            sessionService.saveMessage(session, workspace, MessageRole.USER,
                    request.message(), null, null);

            StringBuilder fullResponse = new StringBuilder();

            cliExecutor.executeStreaming(request.message(), workspacePath, null, event -> {
                try {
                    StreamEvent streamEvent = new StreamEvent(event.getType(), event.getContent(), event.getRole());
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(streamEvent));
                    fullResponse.append(event.getContent());
                } catch (IOException e) {
                    log.warn("Failed to send SSE event", e);
                }
            });

            sessionService.saveMessage(session, workspace, MessageRole.ASSISTANT,
                    fullResponse.toString(), null, null);

            emitter.send(SseEmitter.event().name("done").data("complete"));
            emitter.complete();

        } catch (Exception e) {
            log.error("Streaming chat failed", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(new StreamEvent("error", e.getMessage(), "")));
            } catch (IOException ignored) {}
            emitter.completeWithError(e);
        }
    }

    private String extractTitle(String message) {
        if (message.length() <= 50) return message;
        return message.substring(0, 47) + "...";
    }
}
