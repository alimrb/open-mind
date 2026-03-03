package com.openmind.hagap.infrastructure.security;

import com.openmind.hagap.domain.exception.PathTraversalException;
import com.openmind.hagap.infrastructure.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathValidatorTest {

    @TempDir
    Path tempDir;

    private PathValidator validator;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getWorkspace().setBaseDirectory(tempDir.toString());
        validator = new PathValidator(props);
    }

    @Test
    void shouldAcceptValidPath() {
        Path result = validator.validateAndResolve("my-workspace");
        assertTrue(result.startsWith(tempDir));
    }

    @Test
    void shouldRejectParentTraversal() {
        assertThrows(PathTraversalException.class, () ->
                validator.validateAndResolve("../etc/passwd"));
    }

    @Test
    void shouldRejectDoubleParentTraversal() {
        assertThrows(PathTraversalException.class, () ->
                validator.validateAndResolve("../../secret"));
    }

    @Test
    void shouldAcceptNestedPath() {
        Path result = validator.validateAndResolve("workspace/subdir/file.txt");
        assertTrue(result.startsWith(tempDir));
    }

    @Test
    void shouldRejectAbsolutePath() {
        assertThrows(PathTraversalException.class, () ->
                validator.validateAndResolve("/etc/passwd"));
    }

    @Test
    void shouldValidateWorkspacePath() {
        Path validPath = tempDir.resolve("my-workspace");
        assertDoesNotThrow(() -> validator.validateWorkspacePath(validPath));
    }

    @Test
    void shouldRejectWorkspacePathOutsideBase() {
        Path outsidePath = tempDir.getParent().resolve("outside");
        assertThrows(PathTraversalException.class, () ->
                validator.validateWorkspacePath(outsidePath));
    }

    @Test
    void shouldCheckWithinBaseDirectory() {
        Path inside = tempDir.resolve("inside");
        Path outside = tempDir.getParent().resolve("outside");

        assertTrue(validator.isWithinBaseDirectory(inside));
        assertFalse(validator.isWithinBaseDirectory(outside));
    }
}
