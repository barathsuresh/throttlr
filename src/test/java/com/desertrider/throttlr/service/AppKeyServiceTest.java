package com.desertrider.throttlr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.service.cache.AppKeyCacheService;

class AppKeyServiceTest {
    @Test
    void validateAppKeyReturnsAppByLookupWithoutExpensiveHashCheck() {
        String appKey = "throttlr_live_test-key";
        RecordingPasswordEncoder passwordEncoder = new RecordingPasswordEncoder();
        RecordingAppKeyCacheService appKeyCacheService = new RecordingAppKeyCacheService();
        AppKeyService appKeyService = new AppKeyService(passwordEncoder, "test-app-key-secret", appKeyCacheService);

        App app = App.builder()
                .id("app-1")
                .apiKeyLookup(appKeyService.createLookup(appKey))
                .apiKeyHash("stored-hash")
                .build();
        appKeyCacheService.app = app;

        App result = appKeyService.validateAppKey(appKey);

        assertSame(app, result);
        assertEquals(app.getApiKeyLookup(), appKeyCacheService.requestedLookup);
        assertEquals(0, passwordEncoder.matchesCalls);
    }

    @Test
    void validateAppKeyRejectsOverlongKey() {
        PasswordEncoder passwordEncoder = new RecordingPasswordEncoder();
        AppKeyService appKeyService = new AppKeyService(passwordEncoder, "test-app-key-secret", null);

        assertThrows(IllegalArgumentException.class, () -> appKeyService.validateAppKey("k".repeat(501)));
    }

    private static final class RecordingPasswordEncoder implements PasswordEncoder {
        private int matchesCalls;

        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            matchesCalls++;
            return true;
        }
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
