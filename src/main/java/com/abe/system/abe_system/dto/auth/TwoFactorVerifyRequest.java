package com.abe.system.abe_system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Body cho bước 2 của login (POST /api/auth/2fa/confirm-setup hoặc
 * /api/auth/2fa/verify) - dùng chung 1 DTO vì cùng hình dạng dữ liệu.
 */
@Getter
@Setter
public class TwoFactorVerifyRequest {

    @NotBlank
    private String ticket;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Mã xác thực phải gồm đúng 6 chữ số")
    private String code;
}
