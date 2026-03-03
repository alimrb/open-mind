package com.openmind.hagap.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hagap.opencode")
@Getter
@Setter
public class OpenCodeProperties {

    private String binaryPath = "opencode";
    private String model = "zai/glm-4.7";
    private String agent = "build";
    private String mode = "SPAWN";
    private String serverUrl = "http://opencode:4096";
    private int timeoutSeconds = 120;
}
