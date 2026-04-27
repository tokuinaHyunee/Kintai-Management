package com.example.kintai.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.kintai.dto.LoginRequest;
import com.example.kintai.dto.LoginResponse;
import com.example.kintai.service.AuthService;

import jakarta.validation.Valid;
import java.util.Map;

// 認証コントローラー
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ログイン  POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage() != null ? e.getMessage() : "認証に失敗しました"));
        }
    }

    // トークン更新  POST /api/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "認証が必要です"));
        }
        String loginId = (String) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
        String newToken = authService.refreshToken(loginId, role);
        return ResponseEntity.ok(Map.of("token", newToken, "expiresIn", 1800L));
    }
}
