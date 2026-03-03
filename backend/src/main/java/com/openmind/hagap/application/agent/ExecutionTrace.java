package com.openmind.hagap.application.agent;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ExecutionTrace {

    private final String correlationId;
    private final Instant startedAt;
    private final List<StepTrace> steps = new ArrayList<>();

    public ExecutionTrace(String correlationId) {
        this.correlationId = correlationId;
        this.startedAt = Instant.now();
    }

    public StepTrace startStep(String agent, String action) {
        StepTrace step = new StepTrace(agent, action);
        steps.add(step);
        return step;
    }

    public Duration totalDuration() {
        return Duration.between(startedAt, Instant.now());
    }

    @Getter
    public static class StepTrace {
        private final String agent;
        private final String action;
        private final Instant startedAt;
        private Instant completedAt;
        private boolean success;
        private String error;

        public StepTrace(String agent, String action) {
            this.agent = agent;
            this.action = action;
            this.startedAt = Instant.now();
        }

        public void complete(boolean success, String error) {
            this.completedAt = Instant.now();
            this.success = success;
            this.error = error;
        }

        public Duration duration() {
            Instant end = completedAt != null ? completedAt : Instant.now();
            return Duration.between(startedAt, end);
        }
    }
}
