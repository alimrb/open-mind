package com.openmind.hagap.infrastructure.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CliOutputParserTest {

    private CliOutputParser parser;

    @BeforeEach
    void setUp() {
        parser = new CliOutputParser(new ObjectMapper());
    }

    @Test
    void shouldParseJsonOutput() {
        String output = """
                {"type":"message","content":"Hello world","role":"assistant"}
                {"type":"message","content":" How are you?","role":"assistant"}
                """;

        CliOutput result = parser.parse(output, 0);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getExitCode());
        assertEquals(2, result.getEvents().size());
        assertTrue(result.getContent().contains("Hello world"));
    }

    @Test
    void shouldHandleErrorEvents() {
        String output = """
                {"type":"error","content":"Something went wrong","role":""}
                """;

        CliOutput result = parser.parse(output, 1);

        assertFalse(result.isSuccess());
        assertEquals("Something went wrong", result.getErrorMessage());
    }

    @Test
    void shouldHandleNonJsonLines() {
        String output = "This is plain text output\nAnother line";

        CliOutput result = parser.parse(output, 0);

        assertTrue(result.isSuccess());
        assertTrue(result.getContent().contains("This is plain text output"));
    }

    @Test
    void shouldParseSingleEvent() {
        String line = "{\"type\":\"text\",\"content\":\"token\",\"role\":\"assistant\"}";

        CliOutput.CliEvent event = parser.parseSingleEvent(line);

        assertEquals("text", event.getType());
        assertEquals("token", event.getContent());
        assertEquals("assistant", event.getRole());
    }

    @Test
    void shouldHandleEmptyOutput() {
        CliOutput result = parser.parse("", 0);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getEvents().size());
    }

    @Test
    void shouldHandleMixedOutput() {
        String output = """
                Starting process...
                {"type":"message","content":"Response text","role":"assistant"}
                Done.
                """;

        CliOutput result = parser.parse(output, 0);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getEvents().size());
        assertTrue(result.getContent().contains("Response text"));
    }
}
