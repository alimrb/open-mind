package com.openmind.hagap.infrastructure.cli;

import com.openmind.hagap.infrastructure.config.OpenCodeProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "hagap.opencode.mode", havingValue = "SERVER")
@RequiredArgsConstructor
@Slf4j
public class OpenCodeServerManager {

    private final CliCommandBuilder commandBuilder;
    private final OpenCodeProperties properties;
    private Process serverProcess;

    @EventListener(ApplicationReadyEvent.class)
    public void startServer() {
        try {
            var command = commandBuilder.buildServeCommand();
            log.info("Starting OpenCode server: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command)
                    .redirectErrorStream(true);

            serverProcess = pb.start();
            log.info("OpenCode server started (PID: {})", serverProcess.pid());

        } catch (IOException e) {
            log.error("Failed to start OpenCode server", e);
        }
    }

    @PreDestroy
    public void stopServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            log.info("Stopping OpenCode server");
            serverProcess.destroy();
        }
    }

    public boolean isRunning() {
        return serverProcess != null && serverProcess.isAlive();
    }
}
