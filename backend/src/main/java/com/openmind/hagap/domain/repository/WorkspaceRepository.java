package com.openmind.hagap.domain.repository;

import com.openmind.hagap.domain.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    Optional<Workspace> findByName(String name);

    boolean existsByName(String name);
}
