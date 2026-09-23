package com.abe.system.abe_system.dto.auth;

import com.abe.system.abe_system.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload đăng ký tài khoản mới (POST /api/auth/register).
 * role bắt buộc chọn DATA_OWNER hoặc DATA_USER - không cho tự đăng ký ADMIN
 * (tài khoản ADMIN/KGC phải được khởi tạo sẵn hoặc do ADMIN khác tạo).
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank
    @Email
    private String email;

    @Size(max = 100)
    private String fullName;

    @NotNull(message = "role không được để trống (DATA_OWNER hoặc DATA_USER)")
    private Role role;

    // Phòng ban trực thuộc - tùy chọn, không bắt buộc chọn khi đăng ký.
    private Long departmentId;
}
