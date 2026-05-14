package com.desertrider.throttlr.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.desertrider.throttlr.model.enums.Algorithm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rate limit rule for a client within an app.
 * Client ID can be exact match or glob pattern (e.g., "mobile-*").
 * Compound index ensures one rule per (appId, clientId).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rules")
@CompoundIndex(name = "appId_clientId_idx", def = "{'appId': 1, 'clientId': 1}", unique = true)
public class Rule {

    @Id
    private String id;

    /** App this rule belongs to. */
    private String appId;

    /** Account (tenant) owner of this rule. */
    private String accountId;

    /** Client identifier: exact string or glob pattern like "mobile-*". */
    private String clientId;

    /** Rate limiting algorithm: FIXED_WINDOW, SLIDING_WINDOW, or TOKEN_BUCKET. */
    private Algorithm algorithm;

    /** Max requests allowed per window. */
    private int limitPerWindow;

    /** Time window in milliseconds for counting requests. */
    private long windowMs;

    private long createdAt;

    private long updatedAt;
}
