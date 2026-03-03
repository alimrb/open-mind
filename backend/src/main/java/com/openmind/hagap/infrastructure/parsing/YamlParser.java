package com.openmind.hagap.infrastructure.parsing;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class YamlParser implements DocumentParser {

    @Override
    public boolean supports(String filename) {
        return filename.endsWith(".yml") || filename.endsWith(".yaml");
    }

    @Override
    public String parse(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML file: " + filePath, e);
        }
    }
}
