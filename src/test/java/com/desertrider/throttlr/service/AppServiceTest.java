package com.desertrider.throttlr.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.desertrider.throttlr.dto.request.CreateAppRequest;
import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.model.Rule;
import com.desertrider.throttlr.repository.AppRepository;
import com.desertrider.throttlr.repository.RuleRepository;
import com.desertrider.throttlr.service.cache.AppKeyCacheService;
import com.desertrider.throttlr.service.cache.RuleCacheService;

class AppServiceTest {
    @Test
    void createAppRejectsOverlongName() {
        AppService appService = new AppService(null, null, null, null, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> appService.createApp("account-1", new CreateAppRequest("a".repeat(101))));
    }

    @Test
    void deleteAppDeletesOwnedAppAndItsRules() {
        String accountId = "account-1";
        String appId = "app-1";

        App app = App.builder()
                .id(appId)
                .accountId(accountId)
                .apiKeyLookup("lookup-1")
                .build();

        Rule rule = Rule.builder()
                .appId(appId)
                .clientId("user:123")
                .build();

        RecordingAppRepository appRepository = new RecordingAppRepository(app);
        RecordingRuleRepository ruleRepository = new RecordingRuleRepository(rule);
        RecordingAppKeyCacheService appKeyCacheService = new RecordingAppKeyCacheService();
        RecordingRuleCacheService ruleCacheService = new RecordingRuleCacheService();

        AppService appService = new AppService(
                appRepository.proxy(),
                null,
                ruleRepository.proxy(),
                appKeyCacheService,
                ruleCacheService);

        appService.deleteApp(accountId, appId);

        assertSame(app, appRepository.deletedApp);
        assertSame(appId, ruleRepository.deletedRulesAppId);
        assertSame(app.getApiKeyLookup(), appKeyCacheService.deletedLookup);
        assertSame(appId, ruleCacheService.deletedAppId);
        assertSame(rule.getClientId(), ruleCacheService.deletedClientId);
    }

    private static final class RecordingAppRepository {
        private final App app;
        private App deletedApp;

        private RecordingAppRepository(App app) {
            this.app = app;
        }

        private AppRepository proxy() {
            return (AppRepository) Proxy.newProxyInstance(
                    AppRepository.class.getClassLoader(),
                    new Class<?>[] { AppRepository.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByIdAndAccountId" -> Optional.of(app);
                        case "delete" -> {
                            deletedApp = (App) args[0];
                            yield null;
                        }
                        case "toString" -> "RecordingAppRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    private static final class RecordingRuleRepository {
        private final Rule rule;
        private String deletedRulesAppId;

        private RecordingRuleRepository(Rule rule) {
            this.rule = rule;
        }

        private RuleRepository proxy() {
            return (RuleRepository) Proxy.newProxyInstance(
                    RuleRepository.class.getClassLoader(),
                    new Class<?>[] { RuleRepository.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByAppId" -> List.of(rule);
                        case "deleteByAppId" -> {
                            deletedRulesAppId = (String) args[0];
                            yield null;
                        }
                        case "toString" -> "RecordingRuleRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    private static final class RecordingAppKeyCacheService implements AppKeyCacheService {
        private String deletedLookup;

        @Override
        public Optional<App> findByLookup(String lookup) {
            return Optional.empty();
        }

        @Override
        public void put(App app) {
        }

        @Override
        public void delete(String lookup) {
            deletedLookup = lookup;
        }
    }

    private static final class RecordingRuleCacheService implements RuleCacheService {
        private String deletedAppId;
        private String deletedClientId;

        @Override
        public Optional<Rule> findByAppIdAndClientId(String appId, String clientId) {
            return Optional.empty();
        }

        @Override
        public void put(Rule rule) {
        }

        @Override
        public void delete(String appId, String clientId) {
            deletedAppId = appId;
            deletedClientId = clientId;
        }
    }
}
