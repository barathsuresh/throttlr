package com.desertrider.throttlr.dto.response;

public record AnalyticsResponse(
        String appId,
        long hour,
        long total,
        long allowed,
        long blocked) {
}
