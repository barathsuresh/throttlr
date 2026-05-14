package com.desertrider.throttlr.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/** JWT configuration loaded from app.jwt.* properties. */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    /** HMAC secret for signing/verifying JWT tokens. */
    private String secret;

    /** Token lifetime (e.g., 1h, 24h). */
    private Duration expiration;

    /** Issuer claim value. */
    private String issuer;

    /** Audience claim value. */
    private String audience;
}