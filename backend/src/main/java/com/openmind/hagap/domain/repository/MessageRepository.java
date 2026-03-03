package com.openmind.hagap.domain.repository;

import com.openmind.hagap.domain.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    List<Message> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
