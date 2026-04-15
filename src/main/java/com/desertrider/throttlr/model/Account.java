package com.desertrider.throttlr.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "accounts")
public class Account {
    @Id
    private String id;

    @Indexed(unique = true)
    private String passphraseLookup;
    
    private String passphraseHash;

    private long createdAt;
}
