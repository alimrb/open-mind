package com.openmind.hagap.infrastructure.security;

import com.openmind.hagap.domain.exception.PathTraversalException;
import com.openmind.hagap.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Multi-layer path traversal defense — validates that all workspace paths stay within
 * the configured base directory, even through symlink indirection.
 *
 * <p>Three checks, each catching a different attack vector:
 * <ol>
 *   <li><b>Normalize</b> — removes {@code ..} sequences ({@code /base/../../../etc/passwd} becomes
 *       {@code /etc/passwd}). Without this, the raw path string passes a naive prefix check.</li>
 *   <li><b>startsWith after normalize</b> — confirms the fully resolved path is still under the
 *       allowed base. Catches encoded traversals ({@code %2e%2e}) that survive URL decoding.</li>
 *   <li><b>Symlink resolution via {@code toRealPath()}</b> — follows symlinks to their target.
 *       Prevents creating {@code /base/workspace/evil -> /etc/} and accessing {@code /etc/passwd}
 *       via what looks like a legitimate workspace path. The symlink itself passes the normalize
 *       check, but its target does not.</li>
 * </ol>
 *
 * <p>Called on every CLI execution and file upload — not cached because symlinks can be
 * created between requests. Performance cost is negligible (filesystem stat, no I/O).
 */
@Component
@RequiredArgsConstructor
public class PathValidator {

    private final AppProperties appProperties;

    public Path validateAndResolve(String requestedPath) {
        Path basePath = Path.of(appProperties.getWorkspace().getBaseDirectory()).toAbsolutePath().normalize();
        Path resolved = basePath.resolve(requestedPath).normalize();

        if (!resolved.startsWith(basePath)) {
            throw new PathTraversalException(
                    "Path traversal detected: '" + requestedPath + "' escapes base directory");
        }

        return resolved;
    }

    public void validateWorkspacePath(Path workspacePath) {
        Path basePath = Path.of(appProperties.getWorkspace().getBaseDirectory()).toAbsolutePath().normalize();

        // Check 1+2: normalize removes ".." sequences, then startsWith confirms we're still in bounds
        Path normalized = workspacePath.toAbsolutePath().normalize();
        if (!normalized.startsWith(basePath)) {
            throw new PathTraversalException(
                    "Workspace path escapes base directory: " + workspacePath);
        }

        // Check 3: follow symlinks — a symlink target can escape even if the link itself is in bounds
        if (Files.isSymbolicLink(workspacePath)) {
            try {
                Path realPath = workspacePath.toRealPath();
                if (!realPath.startsWith(basePath)) {
                    throw new PathTraversalException(
                            "Symlink escapes base directory: " + workspacePath + " -> " + realPath);
                }
            } catch (IOException e) {
                throw new PathTraversalException("Cannot resolve symlink: " + workspacePath);
            }
        }
    }

    public boolean isWithinBaseDirectory(Path path) {
        Path basePath = Path.of(appProperties.getWorkspace().getBaseDirectory()).toAbsolutePath().normalize();
        return path.toAbsolutePath().normalize().startsWith(basePath);
    }
}
