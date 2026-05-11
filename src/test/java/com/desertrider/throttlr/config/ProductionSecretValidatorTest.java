package com.desertrider.throttlr.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecretValidatorTest {
    @Test
    void validateAllowsLocalDefaultsOutsideProdProfile() {
        ProductionSecretValidator validator = new ProductionSecretValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "local"),
                "change-this-local-dev-jwt-secret-32b",
                "my-local-dev-lookup-secret",
                "my-local-dev-app-key-lookup-secret",
                "admin-secret-key-change-this");

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validateRejectsPlaceholderSecretsInProdProfile() {
        ProductionSecretValidator validator = new ProductionSecretValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                "change-this-local-dev-jwt-secret-32b",
                "safe-auth-secret",
                "safe-app-key-secret",
                "safe-admin-key");

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void validateAllowsProductionSafeSecretsInProdProfile() {
        ProductionSecretValidator validator = new ProductionSecretValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                "safe-jwt-secret-that-is-long-enough-for-hs256",
                "safe-auth-secret",
                "safe-app-key-secret",
                "safe-admin-key");

        assertDoesNotThrow(validator::validate);
    }
}
