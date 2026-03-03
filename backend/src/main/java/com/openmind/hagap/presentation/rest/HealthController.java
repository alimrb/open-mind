package com.openmind.hagap.presentation.rest;

import com.openmind.hagap.application.dto.HealthResponse;
import com.openmind.hagap.application.dto.HealthResponse.ComponentHealth;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping
    public HealthResponse health() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();

        components.put("postgres", checkPostgres());
        components.put("gateway", new ComponentHealth("UP", "Running"));

        boolean allUp = components.values().stream()
                .allMatch(c -> "UP".equals(c.status()));

        return new HealthResponse(allUp ? "UP" : "DEGRADED", components);
    }

    private ComponentHealth checkPostgres() {
        try (Connection conn = dataSource.getConnection()) {
            conn.isValid(2);
            return new ComponentHealth("UP", "Connected");
        } catch (Exception e) {
            return new ComponentHealth("DOWN", e.getMessage());
        }
    }
}
