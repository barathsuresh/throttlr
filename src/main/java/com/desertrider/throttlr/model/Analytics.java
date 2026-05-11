package com.desertrider.throttlr.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analytics")
@CompoundIndex(name = "appId_hour_idx", def = "{'appId': 1, 'hour': 1}", unique = true)
public class Analytics {

    @Id
    private String id;

    private String appId;

    private long hour;

    private long totalRequests;

    private long allowedRequests;

    private long blockedRequests;

    private long createdAt;

    private long updatedAt;
}
