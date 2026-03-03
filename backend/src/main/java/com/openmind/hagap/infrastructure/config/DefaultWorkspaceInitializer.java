package com.openmind.hagap.infrastructure.config;

import com.openmind.hagap.domain.model.Workspace;
import com.openmind.hagap.domain.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultWorkspaceInitializer {

    private static final String DEFAULT_WORKSPACE_NAME = "OpenMind Knowledge";

    private final WorkspaceRepository workspaceRepository;
    private final AppProperties appProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureDefaultWorkspaceExists() {
        if (!workspaceRepository.findAll().isEmpty()) {
            log.info("Workspaces already exist, skipping default workspace creation");
            return;
        }

        try {
            String dirName = "openmind-knowledge";
            Path workspacePath = Path.of(appProperties.getWorkspace().getBaseDirectory(), dirName);
            Files.createDirectories(workspacePath);

            Workspace workspace = Workspace.builder()
                    .name(DEFAULT_WORKSPACE_NAME)
                    .description("Default workspace with OpenMind website knowledge base")
                    .directoryPath(workspacePath.toString())
                    .build();

            workspace = workspaceRepository.save(workspace);
            log.info("Created default workspace '{}' ({})", workspace.getName(), workspace.getId());
        } catch (IOException e) {
            log.error("Failed to create default workspace directory", e);
        }
    }
}
