package com.openmind.hagap.application.dto;

public record StreamEvent(
        String type,
        String content,
        String role
) {}
