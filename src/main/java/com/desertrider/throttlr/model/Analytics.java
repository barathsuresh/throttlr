package com.desertrider.throttlr.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "analytics")
@CompoundIndex(name = "appId_clientId_hour_idx", def = "{'appId': 1, 'clientId': 1, 'hour': 1}")
public class Analytics {

    @Id
    private String id;

    private String appId;

    private String clientId;

    private long hour;

    private long totalRequests;

    private long blockedRequests;

    private double avgLatencyMs;

    private double p99LatencyMs;
}
