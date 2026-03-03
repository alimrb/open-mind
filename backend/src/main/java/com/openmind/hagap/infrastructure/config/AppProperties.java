package com.openmind.hagap.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hagap")
@Getter
@Setter
public class AppProperties {

    private WorkspaceProperties workspace = new WorkspaceProperties();
    private OpenCodeProperties opencode = new OpenCodeProperties();
    private QdrantProperties qdrant = new QdrantProperties();
    private EmbeddingProperties embedding = new EmbeddingProperties();

    @Getter
    @Setter
    public static class WorkspaceProperties {
        private String baseDirectory = "/tmp/hagap/workspaces";
    }

    @Getter
    @Setter
    public static class EmbeddingProperties {
        private String model = "text-embedding";
    }
}
