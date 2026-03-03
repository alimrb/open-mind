package com.openmind.hagap.application.agent;

import java.util.Map;

/**
 * Strategy interface for agent tools — generic {@code <I, O>} enforces type safety at the boundary
 * so the Orchestrator can't accidentally pass a String where a VerificationInput is expected.
 *
 * <p>The nested {@link ToolResult} is a Railway-oriented result (Either/Result monad) — success or
 * failure, never both. Chose this over checked exceptions because: (1) tool failures are expected
 * flow (e.g., embedding service down → fallback), not exceptional conditions, (2) forces callers
 * to handle the failure path explicitly instead of catching-and-ignoring.
 */
public interface AgentTool<I, O> {

    /** Registry key — must be unique across all implementations. Convention: lowercase, no dots. */
    String name();

    /** Core execution — AgentContext carries workspace path, correlation ID, and session state. */
    O execute(I input, AgentContext context);

    /** Success XOR failure — pattern from functional programming (Rust's Result, Scala's Either). */
    record ToolResult<T>(boolean success, T data, String error) {
        public static <T> ToolResult<T> ok(T data) {
            return new ToolResult<>(true, data, null);
        }

        public static <T> ToolResult<T> fail(String error) {
            return new ToolResult<>(false, null, error);
        }
    }
}
