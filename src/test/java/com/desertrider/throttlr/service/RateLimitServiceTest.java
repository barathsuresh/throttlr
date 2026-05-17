package com.desertrider.throttlr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
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
        RateLimitService rateLimitService = new RateLimitService(null, null, null, null, null, null, null);

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
                analyticsService,
                new PatternMatcher());

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

        @Override
        public List<Rule> findPatternsByAppId(String appId) {
            return List.of();
        }

        @Override
        public void deletePatternCache(String appId) {
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

    @Test
    void patternRuleAppliedOnExactMiss() {
        String appKey = "throttlr_live_test-pattern";

        RecordingAppKeyCacheService appKeyCacheService = new RecordingAppKeyCacheService();
        AppKeyService appKeyService = new AppKeyService(
                Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
                "test-app-key-secret",
                appKeyCacheService);

        App app = App.builder()
                .id("app-pattern")
                .apiKeyLookup(appKeyService.createLookup(appKey))
                .apiKeyHash(appKeyService.hash(appKey))
                .build();
        appKeyCacheService.app = app;

        Rule patternRule = Rule.builder()
                .id("rule-pat")
                .appId(app.getId())
                .clientId("user:*")
                .algorithm(Algorithm.FIXED_WINDOW)
                .limitPerWindow(10)
                .windowMs(60_000)
                .build();

        PatternRuleCacheService ruleCacheService = new PatternRuleCacheService(patternRule);
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
                analyticsService,
                new PatternMatcher());

        rateLimitService.check(appKey, new CheckRequest("user:abc123"));

        assertNotNull(fixedWindowRateLimiter.rule);
        assertEquals("user:abc123", fixedWindowRateLimiter.rule.getClientId());
        assertEquals(10, fixedWindowRateLimiter.rule.getLimitPerWindow());
        assertNotNull(ruleCacheService.cachedRule);
        assertEquals("user:abc123", ruleCacheService.cachedRule.getClientId());
        assertEquals(10, ruleCacheService.cachedRule.getLimitPerWindow());
        assertEquals(Duration.ofSeconds(30), ruleCacheService.cachedTtl);
    }

    @Test
    void catchAllPatternMatchesWhenNoExactRule() {
        String appKey = "throttlr_live_test-catchall";

        RecordingAppKeyCacheService appKeyCacheService2 = new RecordingAppKeyCacheService();
        AppKeyService appKeyService2 = new AppKeyService(
                Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
                "test-app-key-secret",
                appKeyCacheService2);

        App app = App.builder()
                .id("app-catchall")
                .apiKeyLookup(appKeyService2.createLookup(appKey))
                .apiKeyHash(appKeyService2.hash(appKey))
                .build();
        appKeyCacheService2.app = app;

        Rule catchAll = Rule.builder()
                .id("rule-catch")
                .appId(app.getId())
                .clientId("*")
                .algorithm(Algorithm.FIXED_WINDOW)
                .limitPerWindow(5)
                .windowMs(30_000)
                .build();

        PatternRuleCacheService ruleCacheService = new PatternRuleCacheService(catchAll);
        RecordingFixedWindowRateLimiter fixedWindowRateLimiter = new RecordingFixedWindowRateLimiter();
        RecordingTokenBucketRateLimiter tokenBucketRateLimiter = new RecordingTokenBucketRateLimiter();
        RecordingSlidingWindowRateLimiter slidingWindowRateLimiter = new RecordingSlidingWindowRateLimiter();
        RecordingAnalyticsService analyticsService = new RecordingAnalyticsService();

        RateLimitService rateLimitService = new RateLimitService(
                appKeyService2,
                ruleCacheService,
                fixedWindowRateLimiter,
                tokenBucketRateLimiter,
                slidingWindowRateLimiter,
                analyticsService,
                new PatternMatcher());

        CheckResponse response = rateLimitService.check(appKey, new CheckRequest("ip:203.0.113.10"));

        assertNotNull(fixedWindowRateLimiter.rule);
        assertEquals("ip:203.0.113.10", fixedWindowRateLimiter.rule.getClientId());
        assertEquals(5, fixedWindowRateLimiter.rule.getLimitPerWindow());
        assertTrue(response.allowed());
    }

    private static final class PatternRuleCacheService implements RuleCacheService {
        private final Rule patternRule;
        private Rule cachedRule;
        private Duration cachedTtl;

        private PatternRuleCacheService(Rule patternRule) {
            this.patternRule = patternRule;
        }

        @Override
        public Optional<Rule> findByAppIdAndClientId(String appId, String clientId) {
            return Optional.empty();
        }

        @Override
        public List<Rule> findPatternsByAppId(String appId) {
            return List.of(patternRule);
        }

        @Override
        public void put(Rule rule) {
            this.cachedRule = rule;
        }

        @Override
        public void put(Rule rule, Duration ttl) {
            this.cachedRule = rule;
            this.cachedTtl = ttl;
        }

        @Override
        public void delete(String appId, String clientId) {}

        @Override
        public void deletePatternCache(String appId) {}
    }
}
