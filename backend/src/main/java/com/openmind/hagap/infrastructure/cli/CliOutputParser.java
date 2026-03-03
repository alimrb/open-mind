package com.openmind.hagap.infrastructure.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CliOutputParser {

    private final ObjectMapper objectMapper;

    public CliOutput parse(String rawOutput, int exitCode) {
        List<CliOutput.CliEvent> events = new ArrayList<>();
        StringBuilder contentBuilder = new StringBuilder();
        String errorMessage = null;

        for (String line : rawOutput.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            try {
                JsonNode node = objectMapper.readTree(trimmed);
                String type = node.has("type") ? node.get("type").asText() : "unknown";
                String content = node.has("content") ? node.get("content").asText() : "";
                String role = node.has("role") ? node.get("role").asText() : "";

                events.add(CliOutput.CliEvent.builder()
                        .type(type)
                        .content(content)
                        .role(role)
                        .build());

                if ("assistant".equals(role) || "message".equals(type) || "text".equals(type)) {
                    contentBuilder.append(content);
                }

                if ("error".equals(type)) {
                    errorMessage = content;
                }
            } catch (Exception e) {
                log.debug("Non-JSON line from CLI: {}", trimmed);
                contentBuilder.append(trimmed).append("\n");
            }
        }

        boolean success = exitCode == 0 && errorMessage == null;

        return CliOutput.builder()
                .content(contentBuilder.toString().trim())
                .exitCode(exitCode)
                .events(events)
                .success(success)
                .errorMessage(errorMessage)
                .build();
    }

    public CliOutput.CliEvent parseSingleEvent(String line) {
        try {
            JsonNode node = objectMapper.readTree(line.trim());
            return CliOutput.CliEvent.builder()
                    .type(node.has("type") ? node.get("type").asText() : "unknown")
                    .content(node.has("content") ? node.get("content").asText() : "")
                    .role(node.has("role") ? node.get("role").asText() : "")
                    .build();
        } catch (Exception e) {
            return CliOutput.CliEvent.builder()
                    .type("text")
                    .content(line.trim())
                    .role("")
                    .build();
        }
    }
}
