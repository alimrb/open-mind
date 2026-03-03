package com.openmind.hagap.infrastructure.cli;

import com.openmind.hagap.infrastructure.config.OpenCodeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CliCommandBuilderTest {

    private CliCommandBuilder builder;
    private OpenCodeProperties properties;

    @BeforeEach
    void setUp() {
        properties = new OpenCodeProperties();
        properties.setBinaryPath("opencode");
        properties.setModel("zai/glm-4.7");
        properties.setAgent("build");
        properties.setMode("SPAWN");
        properties.setServerUrl("http://opencode:4096");
        builder = new CliCommandBuilder(properties);
    }

    @Test
    void shouldBuildBasicRunCommand() {
        List<String> cmd = builder.buildRunCommand("hello", Path.of("/tmp"), null);

        assertTrue(cmd.contains("opencode"));
        assertTrue(cmd.contains("run"));
        assertTrue(cmd.contains("--model"));
        assertTrue(cmd.contains("zai/glm-4.7"));
        assertTrue(cmd.contains("--agent"));
        assertTrue(cmd.contains("build"));
        assertTrue(cmd.contains("--format"));
        assertTrue(cmd.contains("json"));
        assertTrue(cmd.contains("hello"));
    }

    @Test
    void shouldNotIncludeAttachInSpawnMode() {
        List<String> cmd = builder.buildRunCommand("hello", Path.of("/tmp"), null);
        assertFalse(cmd.contains("--attach"));
    }

    @Test
    void shouldIncludeAttachInServerMode() {
        properties.setMode("SERVER");
        List<String> cmd = builder.buildRunCommand("hello", Path.of("/tmp"), null);
        assertTrue(cmd.contains("--attach"));
        assertTrue(cmd.contains("http://opencode:4096"));
    }

    @Test
    void shouldIncludeFileAttachments() {
        List<Path> files = List.of(Path.of("/tmp/file1.md"), Path.of("/tmp/file2.txt"));
        List<String> cmd = builder.buildRunCommand("hello", Path.of("/tmp"), files);

        long fileCount = cmd.stream().filter(s -> s.equals("--file")).count();
        assertEquals(2, fileCount);
        assertTrue(cmd.contains("/tmp/file1.md"));
        assertTrue(cmd.contains("/tmp/file2.txt"));
    }

    @Test
    void shouldBuildServeCommand() {
        List<String> cmd = builder.buildServeCommand();
        assertEquals(List.of("opencode", "serve", "--port", "4096"), cmd);
    }
}
