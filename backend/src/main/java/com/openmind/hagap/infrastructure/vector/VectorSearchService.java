package com.openmind.hagap.infrastructure.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openmind.hagap.infrastructure.config.QdrantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@Slf4j
public class VectorSearchService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final QdrantProperties qdrantProperties;

    public VectorSearchService(QdrantProperties qdrantProperties, ObjectMapper objectMapper) {
        this.qdrantProperties = qdrantProperties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("http://" + qdrantProperties.getHost() + ":" + qdrantProperties.getPort())
                .build();
    }

    public List<VectorSearchResult> searchSimilar(UUID workspaceId, List<Float> queryVector, int topK) {
        try {
            Map<String, Object> filter = Map.of(
                    "must", List.of(Map.of(
                            "key", "workspace_id",
                            "match", Map.of("value", workspaceId.toString())
                    ))
            );

            Map<String, Object> body = Map.of(
                    "vector", queryVector,
                    "limit", topK,
                    "with_payload", true,
                    "filter", filter
            );

            String response = webClient.post()
                    .uri("/collections/{collection}/points/search", qdrantProperties.getCollectionName())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseSearchResponse(response);

        } catch (Exception e) {
            log.error("Vector search failed", e);
            return Collections.emptyList();
        }
    }

    public void upsert(UUID workspaceId, UUID chunkId, List<Float> vector, Map<String, Object> payload) {
        try {
            payload = new HashMap<>(payload);
            payload.put("workspace_id", workspaceId.toString());

            Map<String, Object> point = Map.of(
                    "id", chunkId.toString(),
                    "vector", vector,
                    "payload", payload
            );

            Map<String, Object> body = Map.of("points", List.of(point));

            webClient.put()
                    .uri("/collections/{collection}/points", qdrantProperties.getCollectionName())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

        } catch (Exception e) {
            log.error("Vector upsert failed for chunk {}", chunkId, e);
        }
    }

    private List<VectorSearchResult> parseSearchResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("result");
            if (results == null || !results.isArray()) return Collections.emptyList();

            List<VectorSearchResult> searchResults = new ArrayList<>();
            for (JsonNode result : results) {
                JsonNode payload = result.get("payload");
                searchResults.add(VectorSearchResult.builder()
                        .chunkId(UUID.fromString(result.get("id").asText()))
                        .content(payload.has("content") ? payload.get("content").asText() : "")
                        .sourceFile(payload.has("source_file") ? payload.get("source_file").asText() : "")
                        .chunkIndex(payload.has("chunk_index") ? payload.get("chunk_index").asInt() : 0)
                        .score(result.has("score") ? result.get("score").asDouble() : 0.0)
                        .build());
            }
            return searchResults;

        } catch (Exception e) {
            log.error("Failed to parse search response", e);
            return Collections.emptyList();
        }
    }
}
