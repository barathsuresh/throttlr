package com.desertrider.throttlr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.desertrider.throttlr.dto.request.CreateRuleRequest;
import com.desertrider.throttlr.dto.response.RuleResponse;
import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.model.Rule;
import com.desertrider.throttlr.model.enums.Algorithm;
import com.desertrider.throttlr.repository.AppRepository;
import com.desertrider.throttlr.repository.RuleRepository;
import com.desertrider.throttlr.service.cache.RuleCacheService;

class RuleServiceTest {
    @Test
    void createRuleRejectsOverlongClientId() {
        RuleService ruleService = new RuleService(null, null, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> ruleService.createRule(
                        "account-1",
                        "app-1",
                        new CreateRuleRequest("c".repeat(201), Algorithm.FIXED_WINDOW, 10, 60_000)));
    }

    @Test
    void deleteRuleDeletesOwnedRuleAndDecrementsRuleCount() {
        String accountId = "account-1";
        String appId = "app-1";
        String clientId = "user:123";

        App app = App.builder()
                .id(appId)
                .accountId(accountId)
                .ruleCount(1)
                .build();

        Rule rule = Rule.builder()
                .appId(appId)
                .accountId(accountId)
                .clientId(clientId)
                .build();

        RecordingAppRepository appRepository = new RecordingAppRepository(app);
        RecordingRuleRepository ruleRepository = new RecordingRuleRepository(rule);
        RecordingRuleCacheService ruleCacheService = new RecordingRuleCacheService();

        RuleService ruleService = new RuleService(ruleRepository.proxy(), appRepository.proxy(), ruleCacheService);

        ruleService.deleteRule(accountId, appId, clientId);

        assertSame(rule, ruleRepository.deletedRule);
        assertSame(app, appRepository.savedApp);
        assertEquals(0, app.getRuleCount());
        assertEquals(appId, ruleCacheService.deletedAppId);
        assertEquals(clientId, ruleCacheService.deletedClientId);
    }

    @Test
    void updateRuleUpdatesOwnedRuleWithoutChangingRuleCount() {
        String accountId = "account-1";
        String appId = "app-1";
        String clientId = "user:123";

        App app = App.builder()
                .id(appId)
                .accountId(accountId)
                .ruleCount(1)
                .build();

        Rule rule = Rule.builder()
                .id("rule-1")
                .appId(appId)
                .accountId(accountId)
                .clientId(clientId)
                .algorithm(Algorithm.FIXED_WINDOW)
                .limitPerWindow(100)
                .windowMs(60_000)
                .createdAt(10)
                .updatedAt(10)
                .build();

        RecordingAppRepository appRepository = new RecordingAppRepository(app);
        RecordingRuleRepository ruleRepository = new RecordingRuleRepository(rule);
        RecordingRuleCacheService ruleCacheService = new RecordingRuleCacheService();
        RuleService ruleService = new RuleService(ruleRepository.proxy(), appRepository.proxy(), ruleCacheService);

        RuleResponse response = ruleService.updateRule(
                accountId,
                appId,
                clientId,
                new CreateRuleRequest(clientId, Algorithm.TOKEN_BUCKET, 50, 30_000));

        assertSame(rule, ruleRepository.savedRule);
        assertEquals(1, app.getRuleCount());
        assertEquals("rule-1", response.ruleId());
        assertEquals(clientId, response.clientId());
        assertEquals(Algorithm.TOKEN_BUCKET, response.algorithm());
        assertEquals(50, response.limitPerWindow());
        assertEquals(30_000, response.windowMs());
        assertEquals(Algorithm.TOKEN_BUCKET, rule.getAlgorithm());
        assertEquals(50, rule.getLimitPerWindow());
        assertEquals(30_000, rule.getWindowMs());
        assertSame(rule, ruleCacheService.cachedRule);
    }

    @Test
    void createRuleRejectsPatternWithRegexSpecialChars() {
        RuleService ruleService = new RuleService(null, null, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> ruleService.createRule(
                        "account-1",
                        "app-1",
                        new CreateRuleRequest("user:[0-9]*", Algorithm.FIXED_WINDOW, 10, 60_000)));
    }

    @Test
    void createRuleInvalidatesPatternCacheForPatternClientId() {
        String accountId = "account-1";
        String appId = "app-1";
        String clientId = "user:*";

        App app = App.builder()
                .id(appId)
                .accountId(accountId)
                .ruleCount(0)
                .build();

        Rule savedRule = Rule.builder()
                .id("rule-1")
                .appId(appId)
                .accountId(accountId)
                .clientId(clientId)
                .algorithm(Algorithm.FIXED_WINDOW)
                .limitPerWindow(10)
                .windowMs(60_000)
                .createdAt(1L)
                .updatedAt(1L)
                .build();

        RuleRepository ruleRepository = (RuleRepository) Proxy.newProxyInstance(
                RuleRepository.class.getClassLoader(),
                new Class<?>[] { RuleRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByAppIdAndClientId" -> Optional.empty(); // no existing rule
                    case "save" -> savedRule;
                    case "toString" -> "PermissiveRuleRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        RecordingAppRepository appRepository = new RecordingAppRepository(app);
        TrackingRuleCacheService ruleCacheService = new TrackingRuleCacheService();

        RuleService ruleService = new RuleService(ruleRepository, appRepository.proxy(), ruleCacheService);
        ruleService.createRule(accountId, appId,
                new CreateRuleRequest(clientId, Algorithm.FIXED_WINDOW, 10, 60_000));

        assertEquals(appId, ruleCacheService.deletedPatternCacheAppId);
    }

    @Test
    void deleteRuleInvalidatesPatternCacheForPatternClientId() {
        String accountId = "account-1";
        String appId = "app-1";
        String clientId = "user:*";

        App app = App.builder()
                .id(appId)
                .accountId(accountId)
                .ruleCount(1)
                .build();

        Rule rule = Rule.builder()
                .appId(appId)
                .accountId(accountId)
                .clientId(clientId)
                .build();

        RecordingAppRepository appRepository = new RecordingAppRepository(app);
        RecordingRuleRepository ruleRepository = new RecordingRuleRepository(rule);
        TrackingRuleCacheService ruleCacheService = new TrackingRuleCacheService();

        RuleService ruleService = new RuleService(ruleRepository.proxy(), appRepository.proxy(), ruleCacheService);
        ruleService.deleteRule(accountId, appId, clientId);

        assertEquals(appId, ruleCacheService.deletedPatternCacheAppId);
    }

    @Test
    void updateRuleInvalidatesPatternCacheForPatternClientId() {
        String accountId = "account-1";
        String appId = "app-1";
        String clientId = "user:*";

        App app = App.builder()
                .id(appId)
                .accountId(accountId)
                .ruleCount(1)
                .build();

        Rule rule = Rule.builder()
                .id("rule-1")
                .appId(appId)
                .accountId(accountId)
                .clientId(clientId)
                .algorithm(Algorithm.FIXED_WINDOW)
                .limitPerWindow(10)
                .windowMs(60_000)
                .createdAt(1L)
                .updatedAt(1L)
                .build();

        RecordingAppRepository appRepository = new RecordingAppRepository(app);
        RecordingRuleRepository ruleRepository = new RecordingRuleRepository(rule);
        TrackingRuleCacheService ruleCacheService = new TrackingRuleCacheService();

        RuleService ruleService = new RuleService(ruleRepository.proxy(), appRepository.proxy(), ruleCacheService);
        ruleService.updateRule(accountId, appId, clientId,
                new CreateRuleRequest(clientId, Algorithm.TOKEN_BUCKET, 50, 30_000));

        assertEquals(appId, ruleCacheService.deletedPatternCacheAppId);
    }

    private static final class RecordingAppRepository {
        private final App app;
        private App savedApp;

        private RecordingAppRepository(App app) {
            this.app = app;
        }

        private AppRepository proxy() {
            return (AppRepository) Proxy.newProxyInstance(
                    AppRepository.class.getClassLoader(),
                    new Class<?>[] { AppRepository.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByIdAndAccountId" -> Optional.of(app);
                        case "save" -> {
                            savedApp = (App) args[0];
                            yield savedApp;
                        }
                        case "toString" -> "RecordingAppRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    private static final class RecordingRuleRepository {
        private final Rule rule;
        private Rule deletedRule;
        private Rule savedRule;

        private RecordingRuleRepository(Rule rule) {
            this.rule = rule;
        }

        private RuleRepository proxy() {
            return (RuleRepository) Proxy.newProxyInstance(
                    RuleRepository.class.getClassLoader(),
                    new Class<?>[] { RuleRepository.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByAppIdAndClientId" -> Optional.of(rule);
                        case "delete" -> {
                            deletedRule = (Rule) args[0];
                            yield null;
                        }
                        case "save" -> {
                            savedRule = (Rule) args[0];
                            yield savedRule;
                        }
                        case "toString" -> "RecordingRuleRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    private static final class RecordingRuleCacheService implements RuleCacheService {
        private Rule cachedRule;
        private String deletedAppId;
        private String deletedClientId;

        @Override
        public Optional<Rule> findByAppIdAndClientId(String appId, String clientId) {
            return Optional.empty();
        }

        @Override
        public void put(Rule rule) {
            cachedRule = rule;
        }

        @Override
        public void delete(String appId, String clientId) {
            deletedAppId = appId;
            deletedClientId = clientId;
        }

        @Override
        public List<Rule> findPatternsByAppId(String appId) {
            return List.of();
        }

        @Override
        public void deletePatternCache(String appId) {
        }
    }

    private static final class TrackingRuleCacheService implements RuleCacheService {
        private Rule cachedRule;
        private String deletedPatternCacheAppId;

        @Override
        public Optional<Rule> findByAppIdAndClientId(String appId, String clientId) {
            return Optional.empty();
        }

        @Override
        public List<Rule> findPatternsByAppId(String appId) {
            return List.of();
        }

        @Override
        public void put(Rule rule) {
            cachedRule = rule;
        }

        @Override
        public void delete(String appId, String clientId) {
        }

        @Override
        public void deletePatternCache(String appId) {
            deletedPatternCacheAppId = appId;
        }
    }
}
