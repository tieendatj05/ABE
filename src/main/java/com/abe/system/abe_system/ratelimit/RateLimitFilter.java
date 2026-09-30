package com.abe.system.abe_system.ratelimit;

import com.abe.system.abe_system.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Chặn spam/DDoS ở các điểm nhạy cảm nhất của hệ thống:
 * - POST /api/auth/login: tối đa {@code rate-limit.login.max-per-minute} request/phút
 *   theo IP (chống dò mật khẩu hàng loạt từ nhiều tài khoản khác nhau - khác với
 *   LoginAttemptService vốn khoá theo TỪNG username).
 * - POST /api/auth/2fa/verify và /api/auth/2fa/confirm-setup: dùng CHUNG mức giới
 *   hạn với login (cùng theo IP) - chống dò mã 6 số TOTP bằng brute-force (chỉ có
 *   1 triệu khả năng, refresh mỗi 30s, nếu không giới hạn thì 1 script có thể thử
 *   hết trong vài giây).
 * - GET /api/files/{id}/download: tối đa {@code rate-limit.download.max-per-minute}
 *   request/phút theo user đã đăng nhập (chống 1 tài khoản hợp lệ bị lợi dụng để
 *   tải hàng loạt, hoặc vét cạn nội dung file bằng script).
 *
 * Đặt SAU {@code JwtAuthenticationFilter} trong chain (xem SecurityConfig) để
 * SecurityContext đã có Authentication trước khi tính key rate-limit theo user
 * cho endpoint download.
 *
 * Có thể tắt hẳn qua {@code rate-limit.enabled=false} - dùng cho test tự động
 * (xem src/test/resources/application.properties) để tránh nhiều test cùng
 * gọi login/download trong 1 bucket dùng chung làm test khác bị 429 giả.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Pattern DOWNLOAD_PATTERN = Pattern.compile("^/api/files/\\d+/download$");

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.login.max-per-minute:5}")
    private int loginMaxPerMinute;

    @Value("${rate-limit.download.max-per-minute:30}")
    private int downloadMaxPerMinute;

    private record Rule(String key, int maxRequest, String message) {
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (enabled) {
            Rule rule = resolveRule(request);
            if (rule != null && !rateLimitService.tryConsume(rule.key(), rule.maxRequest(), Duration.ofMinutes(1))) {
                writeTooManyRequests(response, rule.message());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    @Nullable
    private Rule resolveRule(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if ("POST".equals(method) && "/api/auth/login".equals(uri)) {
            return new Rule(
                    "login:" + clientIp(request),
                    loginMaxPerMinute,
                    "Quá nhiều lượt đăng nhập từ địa chỉ mạng này, vui lòng thử lại sau ít phút"
            );
        }

        if ("POST".equals(method)
                && ("/api/auth/2fa/verify".equals(uri) || "/api/auth/2fa/confirm-setup".equals(uri))) {
            return new Rule(
                    "2fa:" + clientIp(request),
                    loginMaxPerMinute,
                    "Quá nhiều lần thử mã xác thực từ địa chỉ mạng này, vui lòng thử lại sau ít phút"
            );
        }

        if ("GET".equals(method) && DOWNLOAD_PATTERN.matcher(uri).matches()) {
            String username = currentUsername();
            // Chưa xác thực được (token hết hạn/không có) thì để request đi tiếp bình
            // thường - request đó sẽ tự bị chặn 401/403 ở tầng authorization, không
            // cần rate-limit theo IP làm phức tạp thêm ở đây.
            if (username == null) {
                return null;
            }
            return new Rule(
                    "download:" + username,
                    downloadMaxPerMinute,
                    "Bạn đang tải file quá nhanh, vui lòng thử lại sau ít phút"
            );
        }

        return null;
    }

    @Nullable
    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() ? authentication.getName() : null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429); // HttpServletResponse.SC_TOO_MANY_REQUESTS chỉ có từ Servlet 6.1
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), 429, "Too Many Requests", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
