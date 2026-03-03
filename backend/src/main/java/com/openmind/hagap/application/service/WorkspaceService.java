package com.openmind.hagap.application.service;

import com.openmind.hagap.application.dto.WorkspaceCreateRequest;
import com.openmind.hagap.application.dto.WorkspaceResponse;
import com.openmind.hagap.domain.exception.WorkspaceNotFoundException;
import com.openmind.hagap.domain.model.Workspace;
import com.openmind.hagap.domain.repository.WorkspaceRepository;
import com.openmind.hagap.infrastructure.config.AppProperties;
import com.openmind.hagap.infrastructure.security.PathValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final PathValidator pathValidator;
    private final AppProperties appProperties;

    @Transactional
    public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request) {
        String dirName = request.name().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        Path workspacePath = Path.of(appProperties.getWorkspace().getBaseDirectory(), dirName);

        pathValidator.validateWorkspacePath(workspacePath);

        try {
            Files.createDirectories(workspacePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create workspace directory: " + workspacePath, e);
        }

        Workspace workspace = Workspace.builder()
                .name(request.name())
                .description(request.description())
                .directoryPath(workspacePath.toString())
                .build();

        workspace = workspaceRepository.save(workspace);
        log.info("Created workspace: {} at {}", workspace.getName(), workspace.getDirectoryPath());

        return toResponse(workspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listWorkspaces() {
        return workspaceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(UUID id) {
        return toResponse(findWorkspace(id));
    }

    @Transactional
    public void deleteWorkspace(UUID id) {
        Workspace workspace = findWorkspace(id);
        workspaceRepository.delete(workspace);
        log.info("Deleted workspace: {}", workspace.getName());
    }

    public Workspace findWorkspace(UUID id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new WorkspaceNotFoundException(id));
    }

    private WorkspaceResponse toResponse(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getDirectoryPath(),
                workspace.getCreatedAt()
        );
    }
}
