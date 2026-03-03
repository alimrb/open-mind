package com.openmind.hagap.presentation.rest;

import com.openmind.hagap.application.service.SessionService;
import com.openmind.hagap.domain.model.Message;
import com.openmind.hagap.domain.model.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("/workspaces/{workspaceId}/sessions")
    public List<Session> getSessionsByWorkspace(@PathVariable UUID workspaceId) {
        return sessionService.getSessionsByWorkspace(workspaceId);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<Message> getMessagesBySession(@PathVariable UUID sessionId) {
        return sessionService.getMessagesBySession(sessionId);
    }
}
