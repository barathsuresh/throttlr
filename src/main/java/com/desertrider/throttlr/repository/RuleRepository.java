package com.desertrider.throttlr.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.desertrider.throttlr.model.Rule;

/**
 * Data access for rate limit rules. Supports exact-match and pattern queries.
 */
public interface RuleRepository extends MongoRepository<Rule, String> {
    /** Finds all rules for app. */
    List<Rule> findByAppId(String appId);

    /** Finds exact-match rule (app, client). */
    Optional<Rule> findByAppIdAndClientId(String appId, String clientId);

    /** Paginated rules for app. */
    Page<Rule> findByAppId(String appId, Pageable pageable);

    /** Deletes specific rule. */
    void deleteByAppIdAndClientId(String appId, String clientId);

    /** Deletes all rules in app. */
    void deleteByAppId(String appId);

    /** Counts rules in app. */
    long countByAppId(String appId);

    /** Regex query: finds all pattern rules (clientId contains *) for app. */
    @Query("{ 'appId': ?0, 'clientId': { $regex: '\\\\*' } }")
    List<Rule> findPatternsByAppId(String appId);
}
