package com.desertrider.throttlr.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.desertrider.throttlr.model.App;

public interface AppRepository extends MongoRepository<App, String> {
    Page<App> findByAccountId(String accountId, Pageable pageable);

    Optional<App> findByIdAndAccountId(String id, String accountId);

    Optional<App> findByApiKeyLookup(String apiKeyLookup);
}
