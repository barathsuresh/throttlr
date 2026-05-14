package com.desertrider.throttlr.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.desertrider.throttlr.dto.request.CreateRuleRequest;
import com.desertrider.throttlr.dto.response.PagedResponse;
import com.desertrider.throttlr.dto.response.RuleResponse;
import com.desertrider.throttlr.exception.ConflictException;
import com.desertrider.throttlr.exception.ResourceNotFoundException;
import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.model.Rule;
import com.desertrider.throttlr.repository.AppRepository;
import com.desertrider.throttlr.repository.RuleRepository;
import com.desertrider.throttlr.service.cache.RuleCacheService;
import com.desertrider.throttlr.validation.InputLimits;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleService {
    private final RuleRepository ruleRepository;
    private final AppRepository appRepository;
    private final RuleCacheService ruleCacheService;

    /**
     * Creates rate limit rule for specific client:
     * 1. Validates app ownership and rule uniqueness
     * 2. Persists to MongoDB
     * 3. Updates app's rule count
     * 4. Caches rule and invalidates pattern cache if needed
     */
    public RuleResponse createRule(String accountId, String appId, CreateRuleRequest request) {
        validateCreateRuleRequest(request);

        App app = appRepository.findByIdAndAccountId(appId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("App not found"));

        if (ruleRepository.findByAppIdAndClientId(appId, request.clientId().trim()).isPresent()) {
            throw new ConflictException("Rule already exists for this client");
        }

        long now = System.currentTimeMillis();

        Rule rule = Rule.builder()
                .appId(app.getId())
                .accountId(accountId)
                .clientId(request.clientId().trim())
                .algorithm(request.algorithm())
                .limitPerWindow(request.limitPerWindow())
                .windowMs(request.windowMs())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Rule savedRule = ruleRepository.save(rule);

        app.setRuleCount(app.getRuleCount() + 1);
        appRepository.save(app);
        ruleCacheService.put(savedRule);
        deletePatternCacheIfNeeded(appId, savedRule.getClientId());
        log.info("[RULE] Rule created - accountId: [{}], appId: [{}], clientId: [{}], algorithm: {}",
                accountId, appId, savedRule.getClientId(), savedRule.getAlgorithm());

        return toRuleResponse(savedRule);
    }

    /**
     * Retrieves paginated rules for app (sorted by creation date, newest first).
     */
    public PagedResponse<RuleResponse> listRules(String accountId, String appId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }

        appRepository.findByIdAndAccountId(appId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("App not found"));

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Rule> rulePage = ruleRepository.findByAppId(appId, pageable);

        List<RuleResponse> items = rulePage.getContent()
                .stream()
                .map(this::toRuleResponse)
                .toList();

        return new PagedResponse<>(
                items,
                rulePage.getNumber(),
                rulePage.getSize(),
                rulePage.getTotalElements(),
                rulePage.getTotalPages(),
                rulePage.hasNext(),
                rulePage.hasPrevious());
    }

    /**
     * Deletes rule and decrements app's rule count. Clears rule cache and pattern
     * cache if needed.
     */
    public void deleteRule(String accountId, String appId, String clientId) {
        App app = appRepository.findByIdAndAccountId(appId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("App not found"));

        Rule rule = ruleRepository.findByAppIdAndClientId(appId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));

        ruleRepository.delete(rule);
        ruleCacheService.delete(appId, clientId);
        deletePatternCacheIfNeeded(appId, clientId);

        app.setRuleCount(Math.max(0, app.getRuleCount() - 1));
        appRepository.save(app);
        log.info("[RULE] Rule deleted - accountId: [{}], appId: [{}], clientId: [{}]", accountId, appId, clientId);
    }

    /**
     * Updates rule algorithm/limits and invalidates caches. Client ID cannot be
     * changed.
     */
    public RuleResponse updateRule(String accountId, String appId, String clientId, CreateRuleRequest request) {
        validateCreateRuleRequest(request);
        if (!clientId.equals(request.clientId().trim())) {
            throw new IllegalArgumentException("Client id cannot be changed");
        }

        appRepository.findByIdAndAccountId(appId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("App not found"));

        Rule rule = ruleRepository.findByAppIdAndClientId(appId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));

        rule.setAlgorithm(request.algorithm());
        rule.setLimitPerWindow(request.limitPerWindow());
        rule.setWindowMs(request.windowMs());
        rule.setUpdatedAt(System.currentTimeMillis());

        Rule savedRule = ruleRepository.save(rule);
        ruleCacheService.put(savedRule);
        deletePatternCacheIfNeeded(appId, savedRule.getClientId());
        log.info("[RULE] Rule updated - accountId: [{}], appId: [{}], clientId: [{}], algorithm: {}",
                accountId, appId, clientId, savedRule.getAlgorithm());
        return toRuleResponse(savedRule);
    }

    /**
     * If client ID is a pattern (contains wildcards), invalidate all pattern cache
     * for this app.
     */
    private void deletePatternCacheIfNeeded(String appId, String clientId) {
        if (PatternMatcher.isPattern(clientId)) {
            ruleCacheService.deletePatternCache(appId);
        }
    }

    private void validateCreateRuleRequest(CreateRuleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Rule request is required");
        }

        if (!StringUtils.hasText(request.clientId())) {
            throw new IllegalArgumentException("Client id is required");
        }
        InputLimits.requireMaxLength(
                request.clientId(),
                InputLimits.CLIENT_ID_MAX_LENGTH,
                "Client id must be at most 200 characters");
        if (PatternMatcher.isPattern(request.clientId())) {
            PatternMatcher.validatePattern(request.clientId());
        }

        if (request.algorithm() == null) {
            throw new IllegalArgumentException("Algorithm is required");
        }

        if (request.limitPerWindow() <= 0) {
            throw new IllegalArgumentException("Limit per window must be greater than 0");
        }

        if (request.windowMs() <= 0) {
            throw new IllegalArgumentException("Window must be greater than 0");
        }
    }

    private RuleResponse toRuleResponse(Rule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getClientId(),
                rule.getAlgorithm(),
                rule.getLimitPerWindow(),
                rule.getWindowMs());
    }
}
