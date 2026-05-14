package com.desertrider.throttlr.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.desertrider.throttlr.dto.request.CheckRequest;
import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.service.RateLimitService;

import lombok.RequiredArgsConstructor;

/**
 * Public API endpoint for rate limit checks. Accepts app key and client ID,
 * returns allow/deny decision with remaining quota.
 */
@RestController
@RequiredArgsConstructor
public class CheckController {
    private final RateLimitService rateLimitService;

    /**
     * Checks if client request is within rate limits. Returns quota status and
     * current window info.
     */
    @PostMapping("/api/check")
    public CheckResponse check(
            @RequestHeader(value = "X-App-Key", required = false) String appKey,
            @RequestBody CheckRequest request) {
        return rateLimitService.check(appKey, request);
    }
}
