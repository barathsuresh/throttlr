package com.desertrider.throttlr.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class ProductionSecretValidator {
    private static final List<String> UNSAFE_SECRET_VALUES = List.of(
            "change-this-local-dev-jwt-secret-32b",
            "my-local-dev-lookup-secret",
            "my-local-dev-app-key-lookup-secret",
            "admin-secret-key-change-this");

    private final Environment environment;
    private final String jwtSecret;
    private final String authLookupSecret;
    private final String appKeyLookupSecret;
    private final String adminKey;

    public ProductionSecretValidator(
            Environment environment,
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.auth.lookup-secret}") String authLookupSecret,
            @Value("${app.app-key.lookup-secret}") String appKeyLookupSecret,
            @Value("${app.admin.key}") String adminKey) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.authLookupSecret = authLookupSecret;
        this.appKeyLookupSecret = appKeyLookupSecret;
        this.adminKey = adminKey;
    }

    @PostConstruct
    public void validate() {
        if (!List.of(environment.getActiveProfiles()).contains("prod")) {
            return;
        }

        rejectUnsafeSecret(jwtSecret, "JWT_SECRET");
        rejectUnsafeSecret(authLookupSecret, "AUTH_LOOKUP_SECRET");
        rejectUnsafeSecret(appKeyLookupSecret, "APP_KEY_LOOKUP_SECRET");
        rejectUnsafeSecret(adminKey, "ADMIN_KEY");
    }

    private void rejectUnsafeSecret(String value, String envName) {
        if (value == null || value.isBlank() || UNSAFE_SECRET_VALUES.contains(value)) {
            throw new IllegalStateException(envName + " must be configured with a production-safe value");
        }
    }
}
