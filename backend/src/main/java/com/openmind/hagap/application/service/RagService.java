package com.openmind.hagap.application.service;

import com.openmind.hagap.application.dto.ChatRequest;
import com.openmind.hagap.application.dto.ChatResponse;
import com.openmind.hagap.application.dto.CitationDto;
import com.openmind.hagap.domain.model.*;
import com.openmind.hagap.infrastructure.cli.CliOutput;
import com.openmind.hagap.infrastructure.cli.OpenCodeCliExecutor;
import com.openmind.hagap.infrastructure.embedding.EmbeddingService;
import com.openmind.hagap.infrastructure.vector.VectorSearchResult;
import com.openmind.hagap.infrastructure.vector.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private static final int TOP_K = 5;

    private final VectorSearchService vectorSearchService;
    private final EmbeddingService embeddingService;
    private final PromptAssembler promptAssembler;
    private final HallucinationControlService hallucinationControl;
    private final OpenCodeCliExecutor cliExecutor;
    private final WorkspaceService workspaceService;
    private final SessionService sessionService;

    public ChatResponse ragChat(ChatRequest request) {
        Workspace workspace = workspaceService.findWorkspace(request.workspaceId());
        Path workspacePath = Path.of(workspace.getDirectoryPath());

        Session session = sessionService.getOrCreateSession(
                request.sessionId(), workspace, extractTitle(request.message()));

        sessionService.saveMessage(session, workspace, MessageRole.USER,
                request.message(), null, null);

        List<Float> queryEmbedding = embeddingService.generateEmbedding(request.message());

        List<VectorSearchResult> searchResults = vectorSearchService.searchSimilar(
                request.workspaceId(), queryEmbedding, TOP_K);

        double confidence = hallucinationControl.calculateConfidence(searchResults);

        String enrichedPrompt;
        if (hallucinationControl.meetsThreshold(confidence)) {
            enrichedPrompt = promptAssembler.assembleWithContext(request.message(), searchResults);
        } else {
            enrichedPrompt = request.message()
                    + "\n\nNote: Limited knowledge base context available. "
                    + "Answer based on general knowledge but indicate uncertainty.";
        }

        CliOutput output = cliExecutor.execute(enrichedPrompt, workspacePath, null);

        List<CitationDto> citations = searchResults.stream()
                .map(r -> new CitationDto(
                        r.getChunkId(),
                        r.getSourceFile(),
                        r.getChunkIndex(),
                        r.getContent().substring(0, Math.min(200, r.getContent().length())),
                        r.getScore()))
                .toList();

        Message assistantMessage = sessionService.saveMessage(
                session, workspace, MessageRole.ASSISTANT,
                output.getContent(), null, confidence);

        return new ChatResponse(
                assistantMessage.getId(),
                session.getId(),
                output.getContent(),
                confidence,
                citations,
                assistantMessage.getCreatedAt()
        );
    }

    private String extractTitle(String message) {
        if (message.length() <= 50) return message;
        return message.substring(0, 47) + "...";
    }
}
