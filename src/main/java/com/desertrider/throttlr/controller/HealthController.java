package com.desertrider.throttlr.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desertrider.throttlr.dto.response.HealthResponse;
import com.desertrider.throttlr.service.HealthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class HealthController {
    private final HealthService healthService;

    @GetMapping("/api/health")
    public HealthResponse health() {
        return healthService.health();
    }

    @GetMapping("/api/health/redis")
    public HealthResponse redisHealth() {
        return healthService.redisHealth();
    }

    @GetMapping("/api/health/mongo")
    public HealthResponse mongoHealth() {
        return healthService.mongoHealth();
    }
}
