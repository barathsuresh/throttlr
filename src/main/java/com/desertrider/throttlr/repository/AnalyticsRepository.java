package com.desertrider.throttlr.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.desertrider.throttlr.model.Analytics;

public interface AnalyticsRepository extends MongoRepository<Analytics, String> {
    Optional<Analytics> findByAppIdAndHour(String appId, long hour);

    long deleteByHourLessThan(long hour);
}
