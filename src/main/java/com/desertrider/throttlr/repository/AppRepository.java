package com.desertrider.throttlr.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.desertrider.throttlr.model.App;

/**
 * Data access for rate limit apps. Supports account-scoped and key lookup
 * queries.
 */
public interface AppRepository extends MongoRepository<App, String> {
    /** Lists all apps for account (paginated). */
    Page<App> findByAccountId(String accountId, Pageable pageable);

    /** Finds app owned by specific account (prevents cross-account access). */
    Optional<App> findByIdAndAccountId(String id, String accountId);

    /** Finds app by API key lookup hash for fast authentication. */
    Optional<App> findByApiKeyLookup(String apiKeyLookup);
}
