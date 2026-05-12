package com.desertrider.throttlr.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.desertrider.throttlr.dto.request.CheckRequest;
import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.model.Rule;
import com.desertrider.throttlr.model.enums.Algorithm;
import com.desertrider.throttlr.service.analytics.AnalyticsService;
import com.desertrider.throttlr.service.cache.RuleCacheService;
import com.desertrider.throttlr.service.limiter.FixedWindowRateLimiter;
import com.desertrider.throttlr.service.limiter.SlidingWindowRateLimiter;
import com.desertrider.throttlr.service.limiter.TokenBucketRateLimiter;
import com.desertrider.throttlr.validation.InputLimits;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RateLimitService {
    private final AppKeyService appKeyService;
    private final RuleCacheService ruleCacheService;
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;
    private final AnalyticsService analyticsService;
    private final PatternMatcher patternMatcher;

    public CheckResponse check(String appKey, CheckRequest request) {
        if (request == null || !StringUtils.hasText(request.clientId())) {
            throw new IllegalArgumentException("Client id is required");
        }
        InputLimits.requireMaxLength(
                request.clientId(),
                InputLimits.CLIENT_ID_MAX_LENGTH,
                "Client id must be at most 200 characters");

        App app = appKeyService.validateAppKey(appKey);
        String clientId = request.clientId().trim();

        CheckResponse response = ruleCacheService.findByAppIdAndClientId(app.getId(), clientId)
                .or(() -> patternMatcher
                        .findBestMatch(ruleCacheService.findPatternsByAppId(app.getId()), clientId)
                        .map(pattern -> synthesizeRule(pattern, clientId)))
                .map(this::checkConfiguredRule)
                .orElseGet(this::allowWithoutRule);

        analyticsService.record(app, clientId, response);
        return response;
    }

    private Rule synthesizeRule(Rule pattern, String actualClientId) {
        return Rule.builder()
                .id(pattern.getId())
                .appId(pattern.getAppId())
                .accountId(pattern.getAccountId())
                .clientId(actualClientId)
                .algorithm(pattern.getAlgorithm())
                .limitPerWindow(pattern.getLimitPerWindow())
                .windowMs(pattern.getWindowMs())
                .createdAt(pattern.getCreatedAt())
                .updatedAt(pattern.getUpdatedAt())
                .build();
    }

    private CheckResponse checkConfiguredRule(Rule rule) {
        if (rule.getAlgorithm() == Algorithm.FIXED_WINDOW) {
            return fixedWindowRateLimiter.check(rule);
        }

        if (rule.getAlgorithm() == Algorithm.TOKEN_BUCKET) {
            return tokenBucketRateLimiter.check(rule);
        }

        if (rule.getAlgorithm() == Algorithm.SLIDING_WINDOW) {
            return slidingWindowRateLimiter.check(rule);
        }

        throw new IllegalArgumentException("Unsupported rate limit algorithm: " + rule.getAlgorithm());
    }

    private CheckResponse allowWithoutRule() {
        return new CheckResponse(true, Integer.MAX_VALUE, 0, 0);
    }
}
