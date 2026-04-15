package com.desertrider.throttlr.dto.response;

public record CheckResponse(
        boolean allowed,
        int remaining,
        long resetAfterMs,
        long retryAfterMs) {
}
