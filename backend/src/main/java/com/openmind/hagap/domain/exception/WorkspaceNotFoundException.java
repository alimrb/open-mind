package com.openmind.hagap.domain.exception;

import java.util.UUID;

public class WorkspaceNotFoundException extends RuntimeException {

    public WorkspaceNotFoundException(UUID id) {
        super("Workspace not found: " + id);
    }

    public WorkspaceNotFoundException(String name) {
        super("Workspace not found: " + name);
    }
}
