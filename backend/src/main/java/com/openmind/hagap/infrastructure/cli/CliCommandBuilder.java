package com.openmind.hagap.infrastructure.cli;

import com.openmind.hagap.domain.model.ExecutionMode;
import com.openmind.hagap.infrastructure.config.OpenCodeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CliCommandBuilder {

    private final OpenCodeProperties properties;

    public List<String> buildRunCommand(String prompt, Path workingDirectory, List<Path> attachments) {
        List<String> command = new ArrayList<>();
        command.add(properties.getBinaryPath());
        command.add("run");
        command.add("--model");
        command.add(properties.getModel());
        command.add("--agent");
        command.add(properties.getAgent());
        command.add("--format");
        command.add("json");

        if (ExecutionMode.SERVER.name().equals(properties.getMode())) {
            command.add("--attach");
            command.add(properties.getServerUrl());
        }

        if (attachments != null) {
            for (Path file : attachments) {
                command.add("--file");
                command.add(file.toString());
            }
        }

        command.add(prompt);

        return command;
    }

    public List<String> buildServeCommand() {
        List<String> command = new ArrayList<>();
        command.add(properties.getBinaryPath());
        command.add("serve");
        command.add("--port");
        command.add("4096");
        return command;
    }
}
