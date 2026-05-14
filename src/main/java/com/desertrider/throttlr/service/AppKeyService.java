package com.desertrider.throttlr.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.desertrider.throttlr.exception.UnauthorizedException;
import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.service.cache.AppKeyCacheService;
import com.desertrider.throttlr.validation.InputLimits;

@Service
public class AppKeyService {
    private static final Logger log = LoggerFactory.getLogger(AppKeyService.class);
    private static final String KEY_PREFIX = "throttlr_live_";

    private final PasswordEncoder passwordEncoder;
    private final String lookupSecret;
    private final AppKeyCacheService appKeyCacheService;

    public AppKeyService(
            PasswordEncoder passwordEncoder,
            @Value("${app.app-key.lookup-secret}") String lookupSecret,
            AppKeyCacheService appKeyCacheService) {
        this.passwordEncoder = passwordEncoder;
        this.lookupSecret = lookupSecret;
        this.appKeyCacheService = appKeyCacheService;
    }

    /**
     * Generates secure 32-byte random app key with throttlr_live_ prefix. Uses
     * URL-safe Base64 encoding.
     */
    public String generateAppKey() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        return KEY_PREFIX + token;
    }

    /**
     * Creates HMAC-SHA256 lookup hash for fast app key verification without
     * exposing the key.
     */
    public String createLookup(String appKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(
                    lookupSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(key);

            byte[] digest = mac.doFinal(appKey.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create app key lookup", e);
        }
    }

    /** Encodes app key using password encoder (bcrypt) for secure storage. */
    public String hash(String appKey) {
        return passwordEncoder.encode(normalizeForPasswordEncoder(appKey));
    }

    /** Verifies raw app key against stored bcrypt hash. */
    public boolean matches(String rawAppKey, String storedHash) {
        String normalizedAppKey = normalizeForPasswordEncoder(rawAppKey);
        return passwordEncoder.matches(normalizedAppKey, storedHash);
    }

    /**
     * Validates app key by:
     * 1. Checking if key exists (via lookup hash)
     * 2. Verifying hash matches stored value
     * Throws UnauthorizedException if key invalid or not found.
     */
    public App validateAppKey(String appKey) {
        if (!StringUtils.hasText(appKey)) {
            log.warn("[APP-KEY] Validation failed - app key missing");
            throw new UnauthorizedException("App key is required");
        }
        InputLimits.requireMaxLength(
                appKey,
                InputLimits.APP_KEY_MAX_LENGTH,
                "App key must be at most 500 characters");

        App app = appKeyCacheService.findByLookup(createLookup(appKey))
                .orElseThrow(() -> {
                    log.warn("[APP-KEY] Validation failed - app key not found");
                    return new UnauthorizedException("Invalid app key");
                });

        if (!matches(appKey, app.getApiKeyHash())) {
            log.warn("[APP-KEY] Validation failed - hash mismatch for appId: [{}]", app.getId());
            throw new UnauthorizedException("Invalid app key");
        }

        return app;
    }

    private String normalizeForPasswordEncoder(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to normalize app key for hashing", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
