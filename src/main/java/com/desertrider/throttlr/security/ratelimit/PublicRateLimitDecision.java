package com.desertrider.throttlr.security.ratelimit;

import java.time.Duration;

public record PublicRateLimitDecision(boolean allowed, Duration retryAfter) {
    public static PublicRateLimitDecision allow() {
        return new PublicRateLimitDecision(true, Duration.ZERO);
    }

    public static PublicRateLimitDecision blocked(Duration retryAfter) {
        return new PublicRateLimitDecision(false, retryAfter);
    }
}
