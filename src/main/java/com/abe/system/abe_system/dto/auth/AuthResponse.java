package com.abe.system.abe_system.dto.auth;

import com.abe.system.abe_system.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {

    // null khi con dang cho xac thuc 2FA (xem cac truong twoFactor* ben duoi).
    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;
    private String username;
    private Role role;
    private Long departmentId;
    private String departmentName;

    // --- 2FA (chỉ có giá trị khi token == null, bắt buộc với ADMIN/DEPT_ADMIN) ---
    // "SETUP_REQUIRED" (lần đầu, chưa kích hoạt 2FA) hoặc "CODE_REQUIRED" (đã kích
    // hoạt, cần nhập mã 6 số) - null nếu không cần 2FA (đăng nhập thành công luôn).
    private String twoFactorChallenge;
    // Vé tạm gửi kèm mã 6 số ở bước xác nhận (POST /api/auth/2fa/confirm-setup
    // hoặc /api/auth/2fa/verify) - xem PendingTwoFactorService.
    private String twoFactorTicket;
    // Chỉ có khi twoFactorChallenge = SETUP_REQUIRED: ảnh QR dạng data URI, nhúng
    // thẳng vào <img src=...> bên React.
    private String qrCodeDataUri;
    // Chỉ có khi twoFactorChallenge = SETUP_REQUIRED: chuỗi bí mật để nhập tay vào
    // Google Authenticator nếu không quét được QR.
    private String manualEntryKey;
}
