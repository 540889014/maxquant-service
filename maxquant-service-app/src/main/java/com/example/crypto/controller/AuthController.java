package com.example.crypto.controller;

import com.example.crypto.models.ApiResponse;
import com.example.crypto.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 認証コントローラ
 * ユーザログインとJWTトークン発行を処理
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody Map<String, String> credentials) {
        try {
            return ApiResponse.ok(authService.login(credentials.get("username"), credentials.get("password")));
        } catch (RuntimeException e) {
            return ApiResponse.fail(401, e.getMessage());
        }
    }

    @GetMapping("/validate")
    public ApiResponse<Map<String, Boolean>> validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            String token = authorizationHeader.substring(7); // 移除 "Bearer " 前缀
            boolean isValid = authService.validateToken(token);
            return ApiResponse.ok(Map.of("valid", isValid));
        } catch (Exception e) {
            return ApiResponse.fail(401, "Invalid token");
        }
    }
}