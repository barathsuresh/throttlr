package com.desertrider.throttlr.service;

import java.time.Duration;

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
    private static final Duration SYNTHESIZED_RULE_TTL = Duration.ofSeconds(30);

    private final AppKeyService appKeyService;
    private final RuleCacheService ruleCacheService;
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;
    private final AnalyticsService analyticsService;
    /**
     * Main rate limiting orchestrator.
     * Accepts API key and client ID, returns allow/deny decision with quota info.
     */
    private final PatternMatcher patternMatcher;

    public CheckResponse check(String appKey, CheckRequest request) {
        /**
         * Checks if client request is within rate limits:
         * 1. Validates app key
         * 2. Looks up exact-match rule or finds best pattern match
         * 3. Applies rate limiting algorithm (Fixed Window, Token Bucket, or Sliding
         * Window)
         * 4. Records analytics
         * Returns: allowed flag, remaining quota, reset time, retry-after time
         */
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
                        .map(pattern -> cacheSynthesizedRule(synthesizeRule(pattern, clientId))))
                .map(this::checkConfiguredRule)
                .orElseGet(this::allowWithoutRule);

        analyticsService.record(app, clientId, response);
        return response;
    }

    private Rule cacheSynthesizedRule(Rule rule) {
        ruleCacheService.put(rule, SYNTHESIZED_RULE_TTL);
        return rule;
    }

    private Rule synthesizeRule(Rule pattern, String actualClientId) {
        /** Creates rule instance from pattern by substituting actual client ID. */
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
        /** Dispatches rule check to appropriate rate limiter by algorithm type. */
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
        /** Default response when no rule defined: allow with unlimited quota. */
        return new CheckResponse(true, Integer.MAX_VALUE, 0, 0);
    }
}
