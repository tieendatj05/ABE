package com.abe.system.abe_system.controller;

import com.abe.system.abe_system.dto.auth.AuthResponse;
import com.abe.system.abe_system.dto.auth.ChangePasswordRequest;
import com.abe.system.abe_system.dto.auth.LoginRequest;
import com.abe.system.abe_system.dto.auth.RegisterRequest;
import com.abe.system.abe_system.dto.auth.TwoFactorVerifyRequest;
import com.abe.system.abe_system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Bước 2 của login khi ADMIN/DEPT_ADMIN đăng nhập LẦN ĐẦU (chưa kích hoạt
    // 2FA) - xác nhận mã 6 số vừa quét QR để chính thức bật 2FA và nhận JWT.
    @PostMapping("/2fa/confirm-setup")
    public ResponseEntity<AuthResponse> confirmTwoFactorSetup(@Valid @RequestBody TwoFactorVerifyRequest request) {
        return ResponseEntity.ok(authService.confirmTwoFactorSetup(request));
    }

    // Bước 2 của login khi 2FA đã được kích hoạt từ trước - chỉ cần nhập mã 6 số.
    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthResponse> verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyTwoFactor(request));
    }

    // Đổi mật khẩu của CHÍNH người đang đăng nhập (bất kể role) - xem SecurityConfig,
    // path này được carve-out để yêu cầu authenticated() dù nằm dưới /api/auth/**
    // (mặc định permitAll).
    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
