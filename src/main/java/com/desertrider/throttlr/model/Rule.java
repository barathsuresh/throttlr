package com.desertrider.throttlr.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.desertrider.throttlr.model.enums.Algorithm;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "rules")
@CompoundIndex(name = "appId_clientId_idx", def = "{'appId': 1, 'clientId': 1}", unique = true)
public class Rule {

    @Id
    private String id;

    private String appId;

    private String accountId;

    private String clientId;

    private Algorithm algorithm;

    private int limitPerWindow;

    private long windowMs;

    private long createdAt;

    private long updatedAt;
}
