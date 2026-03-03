package com.openmind.hagap.infrastructure.parsing;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Component
public class CodeFileParser implements DocumentParser {

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".java", ".py", ".js", ".ts", ".tsx", ".jsx",
            ".go", ".rs", ".rb", ".kt", ".scala",
            ".c", ".cpp", ".h", ".hpp",
            ".sh", ".bash", ".zsh",
            ".sql", ".xml", ".html", ".css"
    );

    @Override
    public boolean supports(String filename) {
        return CODE_EXTENSIONS.stream().anyMatch(filename::endsWith);
    }

    @Override
    public String parse(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse code file: " + filePath, e);
        }
    }
}
