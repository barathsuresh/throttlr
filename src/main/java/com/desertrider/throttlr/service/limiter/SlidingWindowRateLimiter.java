package com.desertrider.throttlr.service.limiter;

import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.model.Rule;

public interface SlidingWindowRateLimiter {
    CheckResponse check(Rule rule);
}
