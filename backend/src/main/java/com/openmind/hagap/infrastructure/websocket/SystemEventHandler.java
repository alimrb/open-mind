package com.openmind.hagap.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket for server-push events (indexing progress, alerts) — chose WebSocket over SSE here
 * because: (1) we need bidirectional capability for future client→server commands, (2) SSE is
 * already used for chat streaming and mixing two SSE endpoints complicates EventSource management.
 *
 * <p>{@link CopyOnWriteArraySet}: connections change rarely (connect/disconnect) but broadcasts
 * happen frequently — classic read-heavy pattern where COW's snapshot iteration avoids locking.
 * Trade-off: each add/remove copies the entire set, but with &lt;100 concurrent WebSocket clients
 * this is negligible. For 10K+ connections, would switch to a {@code ConcurrentHashMap.newKeySet()}.
 *
 * <p>Silent failure on send: one broken client (network drop, slow consumer) must not block
 * the broadcast to other clients. The catch-and-warn pattern ensures best-effort delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemEventHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket disconnected: {} ({})", session.getId(), status);
    }

    /** Best-effort broadcast — serializes once, sends to all. One failure doesn't block others. */
    public void broadcast(SystemEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.warn("Failed to send WebSocket message to {}", session.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast system event", e);
        }
    }

    public int getActiveConnections() {
        return (int) sessions.stream().filter(WebSocketSession::isOpen).count();
    }
}
