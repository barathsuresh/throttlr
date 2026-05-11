package com.desertrider.throttlr.service.analytics;

public record AnalyticsSnapshot(
        String appId,
        long hour,
        long totalRequests,
        long allowedRequests,
        long blockedRequests) {
}
