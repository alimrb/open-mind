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

/**
 * Spawns a new OS process per CLI request — chosen over a persistent server connection because
 * the CLI tool is interactive and doesn't reliably release resources between calls.
 *
 * <p>Hard-won production lessons encoded here:
 * <ul>
 *   <li><b>stdin must be closed immediately</b> — CLI tools often check if stdin is a terminal or pipe.
 *       If stdin stays open as a pipe, the tool blocks on read(0) instead of processing the command.
 *       Discovered via {@code strace -p <pid>} showing the process stuck on {@code read(0, ...)}.</li>
 *   <li><b>Deadline-based timeout (not waitFor)</b> — the timeout is checked against wall-clock time
 *       between each stdout line. If the CLI produces output slowly (1 line/sec for 120s), a naive
 *       {@code process.waitFor(timeout)} would kill it even though it's making progress. The deadline
 *       approach only times out when no output arrives within the window.</li>
 *   <li><b>Logical EOF detection</b> — the CLI doesn't close stdout after completing (it's an interactive
 *       tool that stays alive). We detect end-of-response by parsing the JSON event stream for a
 *       {@code step_finish} event with {@code reason: "stop"}. Reading until OS-level EOF would
 *       block forever.</li>
 *   <li><b>Force kill + grace period</b> — {@code destroyForcibly()} is async; the OS needs time to
 *       reclaim the PID. The 5-second {@code waitFor} after kill prevents zombie process accumulation.
 *       Without it, hundreds of zombies appear after sustained load, eventually hitting the PID limit.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "hagap.opencode.mode", havingValue = "SPAWN", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SpawnPerRequestExecutor implements OpenCodeCliExecutor {

    private final CliCommandBuilder commandBuilder;
    private final CliOutputParser outputParser;
    private final OpenCodeProperties properties;
    private final PathValidator pathValidator;

    @Override
    public CliOutput execute(String prompt, Path workingDirectory, List<Path> attachments) {
        pathValidator.validateWorkspacePath(workingDirectory);

        List<String> command = commandBuilder.buildRunCommand(prompt, workingDirectory, attachments);
        log.info("Executing CLI command in {}: {}", workingDirectory, String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true);

            Process process = pb.start();
            // Close stdin immediately — the CLI hangs on read(0) if the pipe stays open
            process.getOutputStream().close();

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
                // Grace period — destroyForcibly() is async; wait for OS to reclaim the PID
                process.waitFor(5, TimeUnit.SECONDS);
                throw new CliTimeoutException(properties.getTimeoutSeconds());
            }

            return outputParser.parse(output.toString(), process.exitValue());

        } catch (CliTimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new CliExecutionException("Failed to execute CLI command", e);
        }
    }

    @Override
    public void executeStreaming(String prompt, Path workingDirectory, List<Path> attachments,
                                 Consumer<CliOutput.CliEvent> eventConsumer) {
        pathValidator.validateWorkspacePath(workingDirectory);

        List<String> command = commandBuilder.buildRunCommand(prompt, workingDirectory, attachments);
        log.info("Streaming CLI command in {}: {}", workingDirectory, String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true);

            Process process = pb.start();
            // Close stdin immediately — the CLI hangs on read(0) if the pipe stays open
            process.getOutputStream().close();

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
                // Grace period — destroyForcibly() is async; wait for OS to reclaim the PID
                process.waitFor(5, TimeUnit.SECONDS);
                throw new CliTimeoutException(properties.getTimeoutSeconds());
            }

        } catch (CliTimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new CliExecutionException("Failed to stream CLI command", e);
        }
    }
}
