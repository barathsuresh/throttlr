package com.desertrider.throttlr.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.desertrider.throttlr.dto.request.CreateRuleRequest;
import com.desertrider.throttlr.dto.response.PagedResponse;
import com.desertrider.throttlr.dto.response.RuleResponse;
import com.desertrider.throttlr.security.model.AccountPrincipal;
import com.desertrider.throttlr.service.RuleService;

import lombok.RequiredArgsConstructor;

/**
 * API for managing rate limit rules per app. Rules define limits for specific
 * clients or patterns.
 */
@RestController
@RequiredArgsConstructor
public class RuleController {
    private final RuleService ruleService;

    /** Creates rate limit rule for specific client. */
    @PostMapping("/api/apps/{appId}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public RuleResponse createRule(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String appId,
            @RequestBody CreateRuleRequest request) {
        return ruleService.createRule(principal.accountId(), appId, request);
    }

    /** Lists paginated rules for app. */
    @GetMapping("/api/apps/{appId}/rules")
    public PagedResponse<RuleResponse> listRules(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String appId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ruleService.listRules(principal.accountId(), appId, page, size);
    }

    /** Deletes rule for client. */
    @DeleteMapping("/api/apps/{appId}/rules/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String appId,
            @PathVariable String clientId) {
        ruleService.deleteRule(principal.accountId(), appId, clientId);
    }

    /** Updates rule algorithm/limits. Client ID cannot be changed. */
    @PutMapping("/api/apps/{appId}/rules/{clientId}")
    public RuleResponse updateRule(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String appId,
            @PathVariable String clientId,
            @RequestBody CreateRuleRequest request) {
        return ruleService.updateRule(principal.accountId(), appId, clientId, request);
    }

}