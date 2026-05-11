package com.desertrider.throttlr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.service.cache.AppKeyCacheService;

class AppKeyServiceTest {
    private final PasswordEncoder passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Test
    void validateAppKeyReturnsAppWhenLookupAndHashMatch() {
        String appKey = "throttlr_live_test-key";
        RecordingAppKeyCacheService appKeyCacheService = new RecordingAppKeyCacheService();
        AppKeyService appKeyService = new AppKeyService(passwordEncoder, "test-app-key-secret", appKeyCacheService);

        App app = App.builder()
                .id("app-1")
                .apiKeyLookup(appKeyService.createLookup(appKey))
                .apiKeyHash(appKeyService.hash(appKey))
                .build();
        appKeyCacheService.app = app;

        App result = appKeyService.validateAppKey(appKey);

        assertSame(app, result);
        assertEquals(app.getApiKeyLookup(), appKeyCacheService.requestedLookup);
    }

    @Test
    void validateAppKeyRejectsOverlongKey() {
        AppKeyService appKeyService = new AppKeyService(passwordEncoder, "test-app-key-secret", null);

        assertThrows(IllegalArgumentException.class, () -> appKeyService.validateAppKey("k".repeat(501)));
    }

    private static final class RecordingAppKeyCacheService implements AppKeyCacheService {
        private App app;
        private String requestedLookup;

        @Override
        public Optional<App> findByLookup(String lookup) {
            requestedLookup = lookup;
            return Optional.ofNullable(app);
        }

        @Override
        public void put(App app) {
        }
    }
}
