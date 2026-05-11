package com.desertrider.throttlr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.desertrider.throttlr.model.Rule;

public interface RuleRepository extends MongoRepository<Rule, String> {
    List<Rule> findByAppId(String appId);

    Optional<Rule> findByAppIdAndClientId(String appId, String clientId);

    Page<Rule> findByAppId(String appId, Pageable pageable);

    void deleteByAppIdAndClientId(String appId, String clientId);

    void deleteByAppId(String appId);

    long countByAppId(String appId);
}
