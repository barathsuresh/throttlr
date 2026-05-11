package com.desertrider.throttlr.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.desertrider.throttlr.dto.response.DemoAppResponse;
import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.model.Rule;
import com.desertrider.throttlr.model.enums.Algorithm;
import com.desertrider.throttlr.service.cache.AppKeyCacheService;
import com.desertrider.throttlr.service.cache.RuleCacheService;

@Service
public class DemoService {
    private static final String DEMO_ACCOUNT_ID = "demo";
    private static final String DEMO_CLIENT_ID = "demo-user";
    private static final int DEMO_LIMIT = 10;
    private static final long DEMO_WINDOW_MS = 60_000;

    private final AppKeyService appKeyService;
    private final AppKeyCacheService appKeyCacheService;
    private final RuleCacheService ruleCacheService;
    private final Duration ttl;

    public DemoService(
            AppKeyService appKeyService,
            AppKeyCacheService appKeyCacheService,
            RuleCacheService ruleCacheService,
            @Value("${app.demo.ttl-ms:900000}") long ttlMs) {
        this.appKeyService = appKeyService;
        this.appKeyCacheService = appKeyCacheService;
        this.ruleCacheService = ruleCacheService;
        this.ttl = Duration.ofMillis(ttlMs);
    }

    public DemoAppResponse createDemoAppKey() {
        long now = System.currentTimeMillis();
        String appId = "demo-" + UUID.randomUUID();
        String appKey = appKeyService.generateAppKey();

        App app = App.builder()
                .id(appId)
                .accountId(DEMO_ACCOUNT_ID)
                .name("Temporary Demo App")
                .apiKeyLookup(appKeyService.createLookup(appKey))
                .apiKeyHash(appKeyService.hash(appKey))
                .ruleCount(1)
                .createdAt(now)
                .build();

        Rule rule = Rule.builder()
                .id("demo-rule-" + UUID.randomUUID())
                .appId(appId)
                .accountId(DEMO_ACCOUNT_ID)
                .clientId(DEMO_CLIENT_ID)
                .algorithm(Algorithm.SLIDING_WINDOW)
                .limitPerWindow(DEMO_LIMIT)
                .windowMs(DEMO_WINDOW_MS)
                .createdAt(now)
                .updatedAt(now)
                .build();

        appKeyCacheService.put(app, ttl);
        ruleCacheService.put(rule, ttl);

        return new DemoAppResponse(
                appId,
                appKey,
                DEMO_CLIENT_ID,
                DEMO_LIMIT,
                DEMO_WINDOW_MS,
                ttl.toMillis(),
                "Temporary demo app key created. It expires automatically.");
    }
}
