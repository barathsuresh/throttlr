package com.desertrider.throttlr.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desertrider.throttlr.dto.response.HealthResponse;
import com.desertrider.throttlr.service.HealthService;

import lombok.RequiredArgsConstructor;

/** Health check endpoints for application and dependencies (Redis, MongoDB). */
@RestController
@RequiredArgsConstructor
public class HealthController {
    private final HealthService healthService;

    /** Basic application health check. */
    @GetMapping("/api/health")
    public HealthResponse health() {
        return healthService.health();
    }

    /** Checks Redis connectivity via PING command. */
    @GetMapping("/api/health/redis")
    public HealthResponse redisHealth() {
        return healthService.redisHealth();
    }

    /** Checks MongoDB connectivity via PING command. */
    @GetMapping("/api/health/mongo")
    public HealthResponse mongoHealth() {
        return healthService.mongoHealth();
    }
}
