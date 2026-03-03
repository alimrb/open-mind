package com.openmind.hagap.infrastructure.parsing;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class TextParser implements DocumentParser {

    @Override
    public boolean supports(String filename) {
        return filename.endsWith(".txt") || filename.endsWith(".text");
    }

    @Override
    public String parse(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse text file: " + filePath, e);
        }
    }
}
