package com.desertrider.throttlr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import com.desertrider.throttlr.dto.request.CheckRequest;
import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.model.Rule;
import com.desertrider.throttlr.model.enums.Algorithm;
import com.desertrider.throttlr.service.analytics.AnalyticsService;
import com.desertrider.throttlr.service.cache.AppKeyCacheService;
import com.desertrider.throttlr.service.cache.RuleCacheService;
import com.desertrider.throttlr.service.limiter.FixedWindowRateLimiter;
import com.desertrider.throttlr.service.limiter.SlidingWindowRateLimiter;
import com.desertrider.throttlr.service.limiter.TokenBucketRateLimiter;

class RateLimitServiceTest {
    @Test
    void checkRejectsOverlongClientId() {
        RateLimitService rateLimitService = new RateLimitService(null, null, null, null, null, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> rateLimitService.check("app-key", new CheckRequest("c".repeat(201))));
    }

    @Test
    void checkUsesCachedRuleAndRecordsAnalytics() {
        String appKey = "throttlr_live_test-key";

        RecordingAppKeyCacheService appKeyCacheService = new RecordingAppKeyCacheService();
        AppKeyService appKeyService = new AppKeyService(
                Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
                "test-app-key-secret",
                appKeyCacheService);

        App app = App.builder()
                .id("app-1")
                .apiKeyLookup(appKeyService.createLookup(appKey))
                .apiKeyHash(appKeyService.hash(appKey))
                .build();
        appKeyCacheService.app = app;

        Rule rule = Rule.builder()
                .appId(app.getId())
                .clientId("user:123")
                .algorithm(Algorithm.SLIDING_WINDOW)
                .limitPerWindow(100)
                .windowMs(60_000)
                .build();

        RecordingRuleCacheService ruleCacheService = new RecordingRuleCacheService(rule);
        RecordingFixedWindowRateLimiter fixedWindowRateLimiter = new RecordingFixedWindowRateLimiter();
        RecordingTokenBucketRateLimiter tokenBucketRateLimiter = new RecordingTokenBucketRateLimiter();
        RecordingSlidingWindowRateLimiter slidingWindowRateLimiter = new RecordingSlidingWindowRateLimiter();
        RecordingAnalyticsService analyticsService = new RecordingAnalyticsService();
        RateLimitService rateLimitService = new RateLimitService(
                appKeyService,
                ruleCacheService,
                fixedWindowRateLimiter,
                tokenBucketRateLimiter,
                slidingWindowRateLimiter,
                analyticsService);

        CheckResponse response = rateLimitService.check(appKey, new CheckRequest("user:123"));

        assertTrue(response.allowed());
        assertEquals(99, response.remaining());
        assertEquals(60_000, response.resetAfterMs());
        assertEquals(0, response.retryAfterMs());
        assertEquals(app.getId(), ruleCacheService.appId);
        assertEquals("user:123", ruleCacheService.clientId);
        assertNull(fixedWindowRateLimiter.rule);
        assertNull(tokenBucketRateLimiter.rule);
        assertEquals(rule, slidingWindowRateLimiter.rule);
        assertEquals(app, analyticsService.app);
        assertEquals("user:123", analyticsService.clientId);
        assertEquals(response, analyticsService.response);
    }

    private static final class RecordingAppKeyCacheService implements AppKeyCacheService {
        private App app;

        @Override
        public Optional<App> findByLookup(String lookup) {
            return Optional.ofNullable(app);
        }

        @Override
        public void put(App app) {
        }
    }

    private static final class RecordingRuleCacheService implements RuleCacheService {
        private final Rule rule;
        private String appId;
        private String clientId;

        private RecordingRuleCacheService(Rule rule) {
            this.rule = rule;
        }

        @Override
        public Optional<Rule> findByAppIdAndClientId(String appId, String clientId) {
            this.appId = appId;
            this.clientId = clientId;
            return Optional.of(rule);
        }

        @Override
        public void put(Rule rule) {
        }

        @Override
        public void delete(String appId, String clientId) {
        }
    }

    private static final class RecordingAnalyticsService implements AnalyticsService {
        private App app;
        private String clientId;
        private CheckResponse response;

        @Override
        public void record(App app, String clientId, CheckResponse response) {
            this.app = app;
            this.clientId = clientId;
            this.response = response;
        }
    }

    private static final class RecordingFixedWindowRateLimiter implements FixedWindowRateLimiter {
        private Rule rule;

        @Override
        public CheckResponse check(Rule rule) {
            this.rule = rule;
            return new CheckResponse(true, 99, 60_000, 0);
        }
    }

    private static final class RecordingTokenBucketRateLimiter implements TokenBucketRateLimiter {
        private Rule rule;

        @Override
        public CheckResponse check(Rule rule) {
            this.rule = rule;
            return new CheckResponse(true, 99, 60_000, 0);
        }
    }

    private static final class RecordingSlidingWindowRateLimiter implements SlidingWindowRateLimiter {
        private Rule rule;

        @Override
        public CheckResponse check(Rule rule) {
            this.rule = rule;
            return new CheckResponse(true, 99, 60_000, 0);
        }
    }
}
