package com.desertrider.throttlr.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rate limiting application (tenant).
 * Each app has unique API key for programmatic rate limit checks.
 * Contains rate limit rules for different clients (users, services, etc.).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "apps")
public class App {

    @Id
    private String id;

    /** Account (owner) ID. */
    private String accountId;

    /** Human-readable app name. */
    private String name;

    /** HMAC-SHA256 hash of API key for fast lookup without exposing key. */
    @Indexed(unique = true)
    private String apiKeyLookup;

    /** Bcrypt hash of API key for verification. */
    private String apiKeyHash;

    /** Count of active rate limit rules in this app. */
    private long ruleCount;

    private long createdAt;

}
