package com.desertrider.throttlr.dto.request;

import com.desertrider.throttlr.model.enums.Algorithm;

public record CreateRuleRequest(String clientId, Algorithm algorithm, int limitPerWindow, long windowMs) {

}
