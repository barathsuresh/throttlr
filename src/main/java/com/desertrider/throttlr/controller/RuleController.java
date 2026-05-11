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

@RestController
@RequiredArgsConstructor
public class RuleController {
    private final RuleService ruleService;

    @PostMapping("/api/apps/{appId}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public RuleResponse createRule(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String appId,
            @RequestBody CreateRuleRequest request) {
        return ruleService.createRule(principal.accountId(), appId, request);
    }

    @GetMapping("/api/apps/{appId}/rules")
    public PagedResponse<RuleResponse> listRules(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String appId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ruleService.listRules(principal.accountId(), appId, page, size);
    }

    @DeleteMapping("/api/apps/{appId}/rules/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String appId,
            @PathVariable String clientId) {
        ruleService.deleteRule(principal.accountId(), appId, clientId);
    }

    @PutMapping("/api/apps/{appId}/rules/{clientId}")
    public RuleResponse updateRule(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String appId,
            @PathVariable String clientId,
            @RequestBody CreateRuleRequest request) {
        return ruleService.updateRule(principal.accountId(), appId, clientId, request);
    }

}