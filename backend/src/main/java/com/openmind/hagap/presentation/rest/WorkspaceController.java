package com.openmind.hagap.presentation.rest;

import com.openmind.hagap.application.dto.WorkspaceCreateRequest;
import com.openmind.hagap.application.dto.WorkspaceResponse;
import com.openmind.hagap.application.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse createWorkspace(@Valid @RequestBody WorkspaceCreateRequest request) {
        return workspaceService.createWorkspace(request);
    }

    @GetMapping
    public List<WorkspaceResponse> listWorkspaces() {
        return workspaceService.listWorkspaces();
    }

    @GetMapping("/{id}")
    public WorkspaceResponse getWorkspace(@PathVariable UUID id) {
        return workspaceService.getWorkspace(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkspace(@PathVariable UUID id) {
        workspaceService.deleteWorkspace(id);
    }
}
