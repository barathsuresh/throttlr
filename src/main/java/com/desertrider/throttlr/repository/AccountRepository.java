package com.desertrider.throttlr.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.desertrider.throttlr.model.Account;

/**
 * Data access for user accounts. Uses lookup hash for fast O(1) authentication.
 */
public interface AccountRepository extends MongoRepository<Account, String> {
    /** Finds account by passphrase lookup hash during login. */
    Optional<Account> findByPassphraseLookup(String passphraseLookup);
}
