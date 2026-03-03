package com.openmind.hagap.infrastructure.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public interface OpenCodeCliExecutor {

    CliOutput execute(String prompt, Path workingDirectory, List<Path> attachments);

    void executeStreaming(String prompt, Path workingDirectory, List<Path> attachments,
                          Consumer<CliOutput.CliEvent> eventConsumer);
}
