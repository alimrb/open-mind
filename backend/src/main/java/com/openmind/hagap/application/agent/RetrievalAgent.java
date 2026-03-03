package com.openmind.hagap.application.agent;

import com.openmind.hagap.infrastructure.embedding.EmbeddingService;
import com.openmind.hagap.infrastructure.vector.VectorSearchResult;
import com.openmind.hagap.infrastructure.vector.VectorSearchService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class RetrievalAgent implements AgentTool<String, List<VectorSearchResult>> {

    private static final int TOP_K = 5;

    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;
    private final CircuitBreaker qdrantBreaker;
    private final Timer vectorSearchTimer;

    public RetrievalAgent(EmbeddingService embeddingService,
                          VectorSearchService vectorSearchService,
                          @Qualifier("qdrantCircuitBreaker") CircuitBreaker qdrantBreaker,
                          @Qualifier("vectorSearchTimer") Timer vectorSearchTimer) {
        this.embeddingService = embeddingService;
        this.vectorSearchService = vectorSearchService;
        this.qdrantBreaker = qdrantBreaker;
        this.vectorSearchTimer = vectorSearchTimer;
    }

    @Override
    public String name() {
        return "rag.search";
    }

    @Override
    public List<VectorSearchResult> execute(String query, AgentContext context) {
        return vectorSearchTimer.record(() -> {
            try {
                return qdrantBreaker.executeSupplier(() -> {
                    List<Float> embedding = embeddingService.generateEmbedding(query);
                    return vectorSearchService.searchSimilar(context.getWorkspaceId(), embedding, TOP_K);
                });
            } catch (Exception e) {
                log.error("Retrieval agent failed for workspace {}", context.getWorkspaceId(), e);
                return Collections.emptyList();
            }
        });
    }
}
