package com.openmind.hagap.presentation.rest;

import com.openmind.hagap.application.dto.KnowledgeUploadResponse;
import com.openmind.hagap.application.service.KnowledgeIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeIngestionService knowledgeIngestionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public KnowledgeUploadResponse uploadKnowledge(
            @PathVariable UUID workspaceId,
            @RequestParam("file") MultipartFile file) {
        return knowledgeIngestionService.uploadFile(workspaceId, file);
    }
}
