package com.abe.system.abe_system.dto.auth;

import com.abe.system.abe_system.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload tạo tài khoản - dùng chung cho 2 nơi:
 * - POST /api/auth/register (public, tự đăng ký): AuthService chỉ chấp nhận
 *   role=DATA_USER (sinh viên tự phục vụ).
 * - POST /api/users (ADMIN-only): UserAdminService chấp nhận DATA_OWNER hoặc
 *   DATA_USER (tài khoản giảng viên do nhà trường/ADMIN cấp).
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
            message = "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt"
    )
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
