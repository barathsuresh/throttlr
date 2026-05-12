package com.desertrider.throttlr.dto.response;

import java.util.List;

public record DemoAppResponse(
        String appId,
        String appKey,
        long expiresInMs,
        List<DemoRuleConfig> rules,
        String message) {

    public record DemoRuleConfig(
            String clientId,
            String algorithm,
            int limitPerWindow,
            long windowMs) {
    }
}
