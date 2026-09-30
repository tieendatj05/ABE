package com.abe.system.abe_system.security;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test đơn vị cho JwtService: sinh token, đọc lại username, và các trường hợp
 * token không còn hợp lệ (hết hạn, sai chữ ký, sai user) - đây là mắt xích
 * đầu tiên của chuỗi xác thực, chưa được test trước khi bổ sung các test này.
 */
class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes());

    private UserDetails userDetails(String username) {
        return User.builder().username(username).password("x").authorities(List.of()).build();
    }

    @Test
    void generatedTokenCarriesUsernameAndValidatesForSameUser() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        UserDetails user = userDetails("owner1");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("owner1");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void tokenIsNotValidForADifferentUsername() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken(userDetails("owner1"));

        assertThat(jwtService.isTokenValid(token, userDetails("someoneElse"))).isFalse();
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRET, 1); // hết hạn gần như ngay lập tức
        UserDetails user = userDetails("owner1");
        String token = jwtService.generateToken(user);

        Thread.sleep(10);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, user)).isInstanceOf(Exception.class);
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtService issuedBy = new JwtService(SECRET, 60_000);
        String token = issuedBy.generateToken(userDetails("owner1"));

        String otherSecret = Base64.getEncoder().encodeToString("fedcba9876543210fedcba9876543210".getBytes());
        JwtService verifiedBy = new JwtService(otherSecret, 60_000);

        assertThatThrownBy(() -> verifiedBy.extractUsername(token)).isInstanceOf(SignatureException.class);
    }
}
