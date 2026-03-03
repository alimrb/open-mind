package com.openmind.hagap.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hagap.qdrant")
@Getter
@Setter
public class QdrantProperties {

    private String host = "localhost";
    private int port = 6333;
    private String collectionName = "hagap_knowledge";
    private int vectorSize = 768;
}
