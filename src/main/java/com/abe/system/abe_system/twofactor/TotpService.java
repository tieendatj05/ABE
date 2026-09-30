package com.abe.system.abe_system.twofactor;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * Bọc thư viện dev.samstevens.totp để sinh secret, vẽ QR (định dạng data URI
 * để React hiển thị thẳng bằng thẻ {@code <img>}, không cần thư viện QR ở
 * frontend), và kiểm tra mã 6 số Google Authenticator. Đây là 2FA THẬT (chuẩn
 * RFC 6238 - TOTP), tương thích trực tiếp với app Google Authenticator/Authy/
 * 1Password... không phải mô phỏng.
 */
@Service
public class TotpService {

    private static final String ISSUER = "ABE File Sharing";

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier codeVerifier;

    public TotpService() {
        TimeProvider timeProvider = new SystemTimeProvider();
        this.codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), timeProvider);
    }

    /**
     * Sinh 1 secret Base32 mới, ngẫu nhiên - gọi khi ADMIN/DEPT_ADMIN đăng
     * nhập lần đầu (chưa kích hoạt 2FA).
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * @return data URI ("data:image/png;base64,...") của ảnh QR - nhúng thẳng
     *         vào {@code <img src=...>} bên React, không cần lưu file/endpoint riêng.
     */
    public String generateQrCodeDataUri(String username, String secret) {
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            byte[] imageBytes = qrGenerator.generate(data);
            return "data:" + qrGenerator.getImageMimeType() + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (QrGenerationException e) {
            throw new IllegalStateException("Không tạo được mã QR cho 2FA", e);
        }
    }

    /**
     * @return true nếu mã 6 số khớp với secret tại thời điểm hiện tại (cho phép
     *         lệch ±1 chu kỳ 30s - hành vi mặc định của DefaultCodeVerifier, bù
     *         trừ lệch giờ nhỏ giữa điện thoại và server).
     */
    public boolean verifyCode(String secret, String code) {
        return code != null && !code.isBlank() && codeVerifier.isValidCode(secret, code);
    }
}
