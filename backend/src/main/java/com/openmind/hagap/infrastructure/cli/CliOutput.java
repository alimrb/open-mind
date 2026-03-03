package com.openmind.hagap.infrastructure.cli;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CliOutput {

    private final String content;
    private final int exitCode;
    private final List<CliEvent> events;
    private final boolean success;
    private final String errorMessage;

    @Getter
    @Builder
    public static class CliEvent {
        private final String type;
        private final String content;
        private final String role;
    }
}
