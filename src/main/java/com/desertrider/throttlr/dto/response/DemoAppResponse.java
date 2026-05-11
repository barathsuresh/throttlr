package com.desertrider.throttlr.dto.response;

public record DemoAppResponse(
        String appId,
        String appKey,
        String clientId,
        int limitPerWindow,
        long windowMs,
        long expiresInMs,
        String message) {
}
