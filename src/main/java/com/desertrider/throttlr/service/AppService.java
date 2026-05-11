package com.desertrider.throttlr.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.desertrider.throttlr.dto.request.CreateAppRequest;
import com.desertrider.throttlr.dto.response.AppCreatedResponse;
import com.desertrider.throttlr.dto.response.AppResponse;
import com.desertrider.throttlr.dto.response.PagedResponse;
import com.desertrider.throttlr.exception.ResourceNotFoundException;
import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.repository.AppRepository;
import com.desertrider.throttlr.repository.RuleRepository;
import com.desertrider.throttlr.service.cache.AppKeyCacheService;
import com.desertrider.throttlr.service.cache.RuleCacheService;
import com.desertrider.throttlr.validation.InputLimits;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppService {
    private final AppRepository appRepository;
    private final AppKeyService appKeyService;
    private final RuleRepository ruleRepository;
    private final AppKeyCacheService appKeyCacheService;
    private final RuleCacheService ruleCacheService;

    public AppCreatedResponse createApp(String accountId, CreateAppRequest request) {
        // If request is null or the app name is null raise an exception
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("App name is required");
        }
        InputLimits.requireMaxLength(
                request.name(),
                InputLimits.APP_NAME_MAX_LENGTH,
                "App name must be at most 100 characters");

        String appKey = appKeyService.generateAppKey();
        String apiKeyLookup = appKeyService.createLookup(appKey);
        String apiKeyHash = appKeyService.hash(appKey);

        App app = App.builder()
                .accountId(accountId)
                .name(request.name().trim())
                .apiKeyLookup(apiKeyLookup)
                .apiKeyHash(apiKeyHash)
                .ruleCount(0)
                .createdAt(System.currentTimeMillis())
                .build();

        App savedApp = appRepository.save(app);
        appKeyCacheService.put(savedApp);

        return new AppCreatedResponse(
                savedApp.getId(),
                savedApp.getName(),
                appKey,
                "App created successfully. Save this app key now because it will not be shown again.");
    }

    public PagedResponse<AppResponse> listApps(String accountId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<App> appPage = appRepository.findByAccountId(accountId, pageable);

        List<AppResponse> items = appPage.getContent()
                .stream()
                .map(this::toAppResponse)
                .toList();

        return new PagedResponse<>(
                items,
                appPage.getNumber(),
                appPage.getSize(),
                appPage.getTotalElements(),
                appPage.getTotalPages(),
                appPage.hasNext(),
                appPage.hasPrevious());
    }

    public void deleteApp(String accountId, String appId) {
        App app = appRepository.findByIdAndAccountId(appId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("App not found"));

        ruleRepository.findByAppId(app.getId())
                .forEach(rule -> ruleCacheService.delete(rule.getAppId(), rule.getClientId()));
        appKeyCacheService.delete(app.getApiKeyLookup());
        ruleRepository.deleteByAppId(app.getId());
        appRepository.delete(app);
    }

    private AppResponse toAppResponse(App app) {
        return new AppResponse(
                app.getId(),
                app.getName(),
                app.getRuleCount(),
                app.getCreatedAt());
    }

}
