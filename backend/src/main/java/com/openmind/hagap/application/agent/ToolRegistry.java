package com.openmind.hagap.application.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Service Locator for agent tools — decouples the Orchestrator from concrete tool implementations.
 *
 * <p>Design trade-offs:
 * <ul>
 *   <li>Spring's {@code List<AgentTool>} constructor injection auto-discovers all beans implementing
 *       the interface — adding a new tool only requires {@code @Component}, zero orchestrator changes.</li>
 *   <li>{@link java.util.LinkedHashMap} over HashMap: deterministic iteration order matters for
 *       logging/debugging. The order matches Spring's bean registration sequence.</li>
 *   <li>The unchecked cast in {@code getTool()} is a deliberate trade-off: Java's type erasure means
 *       we can't store {@code AgentTool<String, CliOutput>} and {@code AgentTool<VerificationInput, VerificationResult>}
 *       in the same typed map. The caller knows the concrete types, so the cast is safe by convention.
 *       Alternative: a typesafe heterogeneous container (Bloch Item 33) adds complexity without real safety gain here.</li>
 * </ul>
 */
@Component
@Slf4j
public class ToolRegistry {

    private final Map<String, AgentTool<?, ?>> tools = new LinkedHashMap<>();

    public ToolRegistry(List<AgentTool<?, ?>> agentTools) {
        for (AgentTool<?, ?> tool : agentTools) {
            tools.put(tool.name(), tool);
            log.info("Registered agent tool: {}", tool.name());
        }
    }

    /** Unchecked cast — safe because each call site knows the concrete I/O types (see class doc). */
    @SuppressWarnings("unchecked")
    public <I, O> AgentTool<I, O> getTool(String name) {
        AgentTool<?, ?> tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        return (AgentTool<I, O>) tool;
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    public Set<String> availableTools() {
        return Collections.unmodifiableSet(tools.keySet());
    }
}
