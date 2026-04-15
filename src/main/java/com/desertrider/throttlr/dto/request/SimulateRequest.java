package com.desertrider.throttlr.dto.request;

public record SimulateRequest(String clientId, int requestCount, int delayBetweenMs) {

}
