package com.abe.system.abe_system.twofactor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void generateSecretReturnsNonBlankBase32String() {
        String secret = totpService.generateSecret();

        assertThat(secret).isNotBlank();
        // Base32 chuan chi gom A-Z va 2-7.
        assertThat(secret).matches("^[A-Z2-7]+$");
    }

    @Test
    void generateSecretIsRandomPerCall() {
        assertThat(totpService.generateSecret()).isNotEqualTo(totpService.generateSecret());
    }

    @Test
    void qrCodeDataUriIsAValidPngDataUri() {
        String secret = totpService.generateSecret();

        String dataUri = totpService.generateQrCodeDataUri("admin", secret);

        assertThat(dataUri).startsWith("data:image/png;base64,");
        // Phai giai ma base64 duoc (khong nem loi) va co du lieu.
        String base64Part = dataUri.substring(dataUri.indexOf(',') + 1);
        byte[] decoded = java.util.Base64.getDecoder().decode(base64Part);
        assertThat(decoded.length).isGreaterThan(0);
    }

    @Test
    void verifyCodeAcceptsCurrentlyValidCode() throws Exception {
        String secret = totpService.generateSecret();
        String currentCode = generateCurrentCode(secret);

        assertThat(totpService.verifyCode(secret, currentCode)).isTrue();
    }

    @Test
    void verifyCodeRejectsWrongCode() {
        String secret = totpService.generateSecret();

        assertThat(totpService.verifyCode(secret, "000000")).isFalse();
    }

    @Test
    void verifyCodeRejectsBlankOrNullCode() {
        String secret = totpService.generateSecret();

        assertThat(totpService.verifyCode(secret, null)).isFalse();
        assertThat(totpService.verifyCode(secret, "")).isFalse();
    }

    private String generateCurrentCode(String secret) throws Exception {
        dev.samstevens.totp.code.CodeGenerator codeGenerator = new dev.samstevens.totp.code.DefaultCodeGenerator();
        dev.samstevens.totp.time.TimeProvider timeProvider = new dev.samstevens.totp.time.SystemTimeProvider();
        return codeGenerator.generate(secret, timeProvider.getTime() / 30);
    }
}
