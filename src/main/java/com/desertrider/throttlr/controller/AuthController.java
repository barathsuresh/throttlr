package com.desertrider.throttlr.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.desertrider.throttlr.dto.request.LoginRequest;
import com.desertrider.throttlr.dto.response.LoginResponse;
import com.desertrider.throttlr.dto.response.RegisterResponse;
import com.desertrider.throttlr.security.model.AccountPrincipal;
import com.desertrider.throttlr.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register() {
        return authService.register();
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public Map<String, String> me(@AuthenticationPrincipal AccountPrincipal principal) {
        return Map.of("accountId", principal.accountId());
    }
}