package com.openmind.hagap.presentation.rest;

import com.openmind.hagap.application.dto.ChatRequest;
import com.openmind.hagap.application.dto.ChatResponse;
import com.openmind.hagap.application.dto.ChatStreamRequest;
import com.openmind.hagap.application.service.ChatService;
import com.openmind.hagap.application.service.ChatStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatStreamService chatStreamService;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatStreamRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        chatStreamService.streamChat(request, emitter);
        return emitter;
    }
}
