package com.openmind.hagap.domain.repository;

import com.openmind.hagap.domain.model.FileStatus;
import com.openmind.hagap.domain.model.KnowledgeFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeFileRepository extends JpaRepository<KnowledgeFile, UUID> {

    List<KnowledgeFile> findByWorkspaceId(UUID workspaceId);

    List<KnowledgeFile> findByWorkspaceIdAndStatus(UUID workspaceId, FileStatus status);
}
