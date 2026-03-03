package com.openmind.hagap.domain.repository;

import com.openmind.hagap.domain.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    List<KnowledgeChunk> findByKnowledgeFileId(UUID knowledgeFileId);

    List<KnowledgeChunk> findByWorkspaceId(UUID workspaceId);
}
