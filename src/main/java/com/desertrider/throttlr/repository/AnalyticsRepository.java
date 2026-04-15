package com.desertrider.throttlr.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.desertrider.throttlr.model.Analytics;

public interface AnalyticsRepository extends MongoRepository<Analytics, String> {
    List<Analytics> findByAppIdAndClientId(String appId, String clientId);

    List<Analytics> findByAppId(String appId);
}
