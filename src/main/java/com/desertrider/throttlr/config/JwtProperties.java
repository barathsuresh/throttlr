package com.desertrider.throttlr.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret; // Secret key for signing JWTs
    private Duration expiration; // Expiration time for JWTs in milliseconds
    private String issuer; // Issuer of the JWT
    private String audience; // Audience for the JWT
}
