package com.openmind.hagap.domain.repository;

import com.openmind.hagap.domain.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    List<Session> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);
}
