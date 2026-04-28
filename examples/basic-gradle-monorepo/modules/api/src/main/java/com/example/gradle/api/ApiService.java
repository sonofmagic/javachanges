package com.example.gradle.api;

import com.example.gradle.core.CoreService;

public final class ApiService {
    private final CoreService coreService = new CoreService();

    public String message() {
        return "api:" + coreService.message();
    }
}
