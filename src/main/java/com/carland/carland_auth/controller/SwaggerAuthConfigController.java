package com.carland.carland_auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SwaggerAuthConfigController {

    @GetMapping("/swagger-auth-config")
    public Map<String, Object> config() {
        return Map.of(
                "loginUrl", "/api/v1/users/login",
                "refreshUrl", "/api/v1/users/refresh",
                "acceptLanguage", "az",
                "accessTtlSeconds", 900
        );
    }
}
