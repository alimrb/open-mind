package com.openmind.hagap.application.service;

import com.openmind.hagap.domain.model.*;
import com.openmind.hagap.domain.repository.MessageRepository;
import com.openmind.hagap.domain.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public Session getOrCreateSession(UUID sessionId, Workspace workspace, String title) {
        if (sessionId != null) {
            return sessionRepository.findById(sessionId)
                    .orElseGet(() -> createSession(workspace, title));
        }
        return createSession(workspace, title);
    }

    @Transactional
    public Session createSession(Workspace workspace, String title) {
        Session session = Session.builder()
                .workspace(workspace)
                .title(title != null ? title : "New conversation")
                .build();
        session = sessionRepository.save(session);
        log.info("Created session: {} for workspace: {}", session.getId(), workspace.getName());
        return session;
    }

    @Transactional
    public Message saveMessage(Session session, Workspace workspace, MessageRole role,
                               String content, String citationsJson, Double confidence) {
        Message message = Message.builder()
                .session(session)
                .workspace(workspace)
                .role(role)
                .content(content)
                .citationsJson(citationsJson)
                .confidence(confidence)
                .build();
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Session> getSessionsByWorkspace(UUID workspaceId) {
        return sessionRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessagesBySession(UUID sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}
