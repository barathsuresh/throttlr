package com.desertrider.throttlr.security.ratelimit;

import java.time.Duration;

public record PublicRateLimitPolicy(String name, long maxRequests, Duration window) {
}
