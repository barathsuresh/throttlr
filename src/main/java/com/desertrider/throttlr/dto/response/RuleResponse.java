package com.desertrider.throttlr.dto.response;

import com.desertrider.throttlr.model.enums.Algorithm;

public record RuleResponse(String ruleId,
        String clientId,
        Algorithm algorithm,
        int limitPerWindow,
        long windowMs) {

}
