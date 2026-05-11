package com.desertrider.throttlr.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.desertrider.throttlr.dto.request.CheckRequest;
import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.service.RateLimitService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CheckController {
    private final RateLimitService rateLimitService;

    @PostMapping("/api/check")
    public CheckResponse check(
            @RequestHeader(value = "X-App-Key", required = false) String appKey,
            @RequestBody CheckRequest request) {
        return rateLimitService.check(appKey, request);
    }
}
