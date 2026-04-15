package com.desertrider.throttlr.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document(collection = "apps")
public class App {

    @Id
    private String id;

    private String accountId;

    private String name;

    @Indexed(unique = true)
    private String apiKeyHash;

    
}
