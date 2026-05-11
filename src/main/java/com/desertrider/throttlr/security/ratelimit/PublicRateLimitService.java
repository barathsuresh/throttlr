package com.desertrider.throttlr.security.ratelimit;

public interface PublicRateLimitService {
    PublicRateLimitDecision check(PublicRateLimitPolicy policy, String clientIp);
}
