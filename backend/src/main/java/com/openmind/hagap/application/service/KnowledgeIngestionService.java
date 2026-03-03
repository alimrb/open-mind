package com.openmind.hagap.application.service;

import com.openmind.hagap.application.dto.KnowledgeUploadResponse;
import com.openmind.hagap.domain.model.*;
import com.openmind.hagap.domain.repository.KnowledgeChunkRepository;
import com.openmind.hagap.domain.repository.KnowledgeFileRepository;
import com.openmind.hagap.infrastructure.embedding.EmbeddingService;
import com.openmind.hagap.infrastructure.parsing.ChunkingStrategy;
import com.openmind.hagap.infrastructure.parsing.DocumentParser;
import com.openmind.hagap.infrastructure.parsing.FixedSizeChunker;
import com.openmind.hagap.infrastructure.vector.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIngestionService {

    private final List<DocumentParser> parsers;
    private final FixedSizeChunker chunker;
    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;
    private final KnowledgeFileRepository knowledgeFileRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final WorkspaceService workspaceService;

    @Transactional
    public KnowledgeUploadResponse uploadFile(UUID workspaceId, MultipartFile file) {
        Workspace workspace = workspaceService.findWorkspace(workspaceId);
        String filename = file.getOriginalFilename();
        Path workspacePath = Path.of(workspace.getDirectoryPath());
        Path filePath = workspacePath.resolve("knowledge").resolve(filename);

        try {
            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save uploaded file", e);
        }

        String fileType = getFileExtension(filename);

        KnowledgeFile knowledgeFile = KnowledgeFile.builder()
                .workspace(workspace)
                .filename(filename)
                .filePath(filePath.toString())
                .fileType(fileType)
                .status(FileStatus.PROCESSING)
                .build();
        knowledgeFile = knowledgeFileRepository.save(knowledgeFile);

        processFileAsync(knowledgeFile.getId(), workspaceId);

        return new KnowledgeUploadResponse(
                knowledgeFile.getId(),
                filename,
                knowledgeFile.getStatus().name(),
                0
        );
    }

    @Async
    public void processFileAsync(UUID fileId, UUID workspaceId) {
        try {
            KnowledgeFile knowledgeFile = knowledgeFileRepository.findById(fileId).orElseThrow();
            Workspace workspace = workspaceService.findWorkspace(workspaceId);
            Path filePath = Path.of(knowledgeFile.getFilePath());

            DocumentParser parser = parsers.stream()
                    .filter(p -> p.supports(knowledgeFile.getFilename()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No parser for file: " + knowledgeFile.getFilename()));

            String content = parser.parse(filePath);
            List<String> chunks = chunker.chunk(content);

            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);

                KnowledgeChunk chunk = KnowledgeChunk.builder()
                        .knowledgeFile(knowledgeFile)
                        .workspace(workspace)
                        .chunkIndex(i)
                        .content(chunkContent)
                        .tokenCount(chunker.estimateTokenCount(chunkContent))
                        .build();
                chunk = knowledgeChunkRepository.save(chunk);

                List<Float> embedding = embeddingService.generateEmbedding(chunkContent);

                Map<String, Object> payload = new HashMap<>();
                payload.put("content", chunkContent);
                payload.put("source_file", knowledgeFile.getFilename());
                payload.put("chunk_index", i);

                vectorSearchService.upsert(workspaceId, chunk.getId(), embedding, payload);
            }

            knowledgeFile.setChunkCount(chunks.size());
            knowledgeFile.setStatus(FileStatus.READY);
            knowledgeFileRepository.save(knowledgeFile);

            log.info("Processed knowledge file '{}': {} chunks", knowledgeFile.getFilename(), chunks.size());

        } catch (Exception e) {
            log.error("Failed to process knowledge file {}", fileId, e);
            knowledgeFileRepository.findById(fileId).ifPresent(f -> {
                f.setStatus(FileStatus.FAILED);
                knowledgeFileRepository.save(f);
            });
        }
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1) : "unknown";
    }
}
