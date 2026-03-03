package com.openmind.hagap.application.agent;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ExecutionPlan {

    public enum PlanType { SIMPLE, MULTI_AGENT }

    private final PlanType type;
    private final List<PlanStep> steps;
    private final String reasoning;

    @Getter
    @Builder
    public static class PlanStep {
        private final String agent;
        private final String action;
        private final String description;
    }
}
