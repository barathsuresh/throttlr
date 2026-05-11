package com.desertrider.throttlr.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.desertrider.throttlr.dto.response.DemoAppResponse;
import com.desertrider.throttlr.service.DemoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DemoController {
    private final DemoService demoService;

    @PostMapping("/api/demo/app-key")
    @ResponseStatus(HttpStatus.CREATED)
    public DemoAppResponse createDemoAppKey() {
        return demoService.createDemoAppKey();
    }
}