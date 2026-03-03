package com.openmind.hagap.application.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolRegistryTest {

    @Test
    void shouldRegisterAndRetrieveTools() {
        AgentTool<String, String> tool1 = createMockTool("tool.alpha");
        AgentTool<String, String> tool2 = createMockTool("tool.beta");

        ToolRegistry registry = new ToolRegistry(List.of(tool1, tool2));

        assertThat(registry.hasTool("tool.alpha")).isTrue();
        assertThat(registry.hasTool("tool.beta")).isTrue();
        assertThat(registry.hasTool("tool.gamma")).isFalse();
    }

    @Test
    void shouldReturnRegisteredTool() {
        AgentTool<String, String> tool = createMockTool("my.tool");
        ToolRegistry registry = new ToolRegistry(List.of(tool));

        AgentTool<String, String> retrieved = registry.getTool("my.tool");
        assertThat(retrieved.name()).isEqualTo("my.tool");
    }

    @Test
    void shouldThrowForUnknownTool() {
        ToolRegistry registry = new ToolRegistry(List.of());

        assertThatThrownBy(() -> registry.getTool("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void shouldListAvailableTools() {
        AgentTool<String, String> tool1 = createMockTool("rag.search");
        AgentTool<String, String> tool2 = createMockTool("opencode.run");
        AgentTool<String, String> tool3 = createMockTool("verify.grounded");

        ToolRegistry registry = new ToolRegistry(List.of(tool1, tool2, tool3));

        assertThat(registry.availableTools())
                .containsExactlyInAnyOrder("rag.search", "opencode.run", "verify.grounded");
    }

    @Test
    void shouldHandleEmptyToolList() {
        ToolRegistry registry = new ToolRegistry(List.of());

        assertThat(registry.availableTools()).isEmpty();
        assertThat(registry.hasTool("anything")).isFalse();
    }

    private AgentTool<String, String> createMockTool(String toolName) {
        return new AgentTool<>() {
            @Override
            public String name() {
                return toolName;
            }

            @Override
            public String execute(String input, AgentContext context) {
                return "result";
            }
        };
    }
}
