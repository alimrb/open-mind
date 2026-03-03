package com.openmind.hagap.infrastructure.cli;

import com.openmind.hagap.domain.exception.CliExecutionException;
import com.openmind.hagap.domain.exception.CliTimeoutException;
import com.openmind.hagap.infrastructure.config.OpenCodeProperties;
import com.openmind.hagap.infrastructure.security.PathValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "hagap.opencode.mode", havingValue = "SERVER")
@RequiredArgsConstructor
@Slf4j
public class AttachToServerExecutor implements OpenCodeCliExecutor {

    private final CliCommandBuilder commandBuilder;
    private final CliOutputParser outputParser;
    private final OpenCodeProperties properties;
    private final PathValidator pathValidator;

    @Override
    public CliOutput execute(String prompt, Path workingDirectory, List<Path> attachments) {
        pathValidator.validateWorkspacePath(workingDirectory);

        List<String> command = commandBuilder.buildRunCommand(prompt, workingDirectory, attachments);
        log.info("Attaching to server for CLI command in {}", workingDirectory);

        try {
            ProcessBuilder pb = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new CliTimeoutException(properties.getTimeoutSeconds());
            }

            return outputParser.parse(output.toString(), process.exitValue());

        } catch (CliTimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new CliExecutionException("Failed to execute CLI command via server", e);
        }
    }

    @Override
    public void executeStreaming(String prompt, Path workingDirectory, List<Path> attachments,
                                 Consumer<CliOutput.CliEvent> eventConsumer) {
        pathValidator.validateWorkspacePath(workingDirectory);

        List<String> command = commandBuilder.buildRunCommand(prompt, workingDirectory, attachments);
        log.info("Streaming via server for CLI command in {}", workingDirectory);

        try {
            ProcessBuilder pb = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    CliOutput.CliEvent event = outputParser.parseSingleEvent(line);
                    eventConsumer.accept(event);
                }
            }

            boolean completed = process.waitFor(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new CliTimeoutException(properties.getTimeoutSeconds());
            }

        } catch (CliTimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new CliExecutionException("Failed to stream CLI command via server", e);
        }
    }
}
