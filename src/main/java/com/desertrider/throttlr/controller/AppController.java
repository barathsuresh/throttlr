package com.desertrider.throttlr.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.desertrider.throttlr.dto.request.CreateAppRequest;
import com.desertrider.throttlr.dto.response.AnalyticsResponse;
import com.desertrider.throttlr.dto.response.AppCreatedResponse;
import com.desertrider.throttlr.dto.response.AppResponse;
import com.desertrider.throttlr.dto.response.PagedResponse;
import com.desertrider.throttlr.security.model.AccountPrincipal;
import com.desertrider.throttlr.service.AnalyticsReadService;
import com.desertrider.throttlr.service.AppService;

import lombok.RequiredArgsConstructor;

/** API for managing rate limiting applications (rate limit "projects"). */
@RestController
@RequestMapping("/api/apps")
@RequiredArgsConstructor
public class AppController {
    private final AppService appService;
    private final AnalyticsReadService analyticsReadService;

    /** Creates new app with auto-generated unique API key. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppCreatedResponse createApp(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestBody CreateAppRequest request) {
        return appService.createApp(principal.accountId(), request);
    }

    /** Lists all apps for authenticated account (paginated). */
    @GetMapping
    public PagedResponse<AppResponse> listApps(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return appService.listApps(principal.accountId(), page, size);
    }

    /** Deletes app and all associated rules. */
    @DeleteMapping("/{appId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApp(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String appId) {
        appService.deleteApp(principal.accountId(), appId);
    }

    /** Rotates app key — invalidates current key and issues a new one (shown once). */
    @PostMapping("/{appId}/rotate-key")
    public AppCreatedResponse rotateAppKey(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String appId) {
        return appService.rotateAppKey(principal.accountId(), appId);
    }

    /** Retrieves current hour analytics: total, allowed, and blocked requests. */
    @GetMapping("/{appId}/analytics")
    public AnalyticsResponse getAnalytics(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String appId) {
        return analyticsReadService.getCurrentHourAnalytics(principal.accountId(), appId);
    }

}
