package com.openmind.hagap.infrastructure.vector;

import com.openmind.hagap.infrastructure.config.QdrantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@Slf4j
public class QdrantCollectionManager {

    private final WebClient webClient;
    private final QdrantProperties qdrantProperties;

    public QdrantCollectionManager(QdrantProperties qdrantProperties) {
        this.qdrantProperties = qdrantProperties;
        this.webClient = WebClient.builder()
                .baseUrl("http://" + qdrantProperties.getHost() + ":" + qdrantProperties.getPort())
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureCollectionExists() {
        try {
            Boolean exists = webClient.get()
                    .uri("/collections/{collection}", qdrantProperties.getCollectionName())
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(r -> true)
                    .onErrorReturn(false)
                    .block();

            if (Boolean.FALSE.equals(exists)) {
                createCollection();
            } else {
                log.info("Qdrant collection '{}' already exists", qdrantProperties.getCollectionName());
            }

        } catch (Exception e) {
            log.warn("Could not verify Qdrant collection, will retry on first use: {}", e.getMessage());
        }
    }

    private void createCollection() {
        try {
            Map<String, Object> body = Map.of(
                    "vectors", Map.of(
                            "size", qdrantProperties.getVectorSize(),
                            "distance", "Cosine"
                    )
            );

            webClient.put()
                    .uri("/collections/{collection}", qdrantProperties.getCollectionName())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Created Qdrant collection '{}'", qdrantProperties.getCollectionName());

        } catch (Exception e) {
            log.error("Failed to create Qdrant collection", e);
        }
    }
}
