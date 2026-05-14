package com.desertrider.throttlr.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hourly analytics snapshot for an app.
 * Persisted from Redis to MongoDB on hourly basis.
 * Compound index ensures one analytics record per (app, hour).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analytics")
@CompoundIndex(name = "appId_hour_idx", def = "{'appId': 1, 'hour': 1}", unique = true)
public class Analytics {

    @Id
    private String id;

    /** App ID for this analytics. */
    private String appId;

    /** UTC hour epoch (milliseconds). */
    private long hour;

    /** Total requests in this hour. */
    private long totalRequests;

    /** Requests allowed by rate limiting. */
    private long allowedRequests;

    /** Requests blocked by rate limiting. */
    private long blockedRequests;

    private long createdAt;

    private long updatedAt;
}
