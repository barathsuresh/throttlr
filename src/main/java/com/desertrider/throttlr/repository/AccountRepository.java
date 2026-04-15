package com.desertrider.throttlr.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.desertrider.throttlr.model.Account;

public interface AccountRepository extends MongoRepository<Account, String> {
    Optional<Account> findByPassphraseLookup(String passphraseLookup);
}
