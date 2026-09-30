package com.abe.system.abe_system.service;

import com.abe.system.abe_system.exception.AccountLockedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chống dò mật khẩu (brute-force) bằng cách đếm số lần đăng nhập sai liên
 * tiếp theo username, tạm khoá 1 khoảng thời gian nếu vượt ngưỡng. Lưu thuần
 * trong bộ nhớ (ConcurrentHashMap) - đủ dùng cho 1 instance app của đồ án,
 * không cần Redis/DB riêng. Mất dữ liệu đếm khi restart app là chấp nhận được
 * (không phải yêu cầu bảo mật cứng, chỉ để giảm thiểu brute-force khi app
 * đang chạy liên tục).
 */
@Service
public class LoginAttemptService {

    private record Attempt(int failCount, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, Attempt> attemptsByUsername = new ConcurrentHashMap<>();

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.lockout-minutes:15}")
    private long lockoutMinutes;

    public void assertNotLocked(String username) {
        Attempt attempt = attemptsByUsername.get(normalize(username));
        if (attempt == null || attempt.lockedUntil() == null) {
            return;
        }
        Instant now = Instant.now();
        if (now.isBefore(attempt.lockedUntil())) {
            long remainingMinutes = Duration.between(now, attempt.lockedUntil()).toMinutes() + 1;
            throw new AccountLockedException(
                    "Tài khoản tạm khoá do đăng nhập sai quá " + maxAttempts
                            + " lần liên tiếp - thử lại sau khoảng " + remainingMinutes + " phút");
        }
    }

    public void recordFailure(String username) {
        attemptsByUsername.compute(normalize(username), (key, current) -> {
            int failCount = (current == null ? 0 : current.failCount()) + 1;
            Instant lockedUntil = failCount >= maxAttempts
                    ? Instant.now().plus(Duration.ofMinutes(lockoutMinutes))
                    : null;
            return new Attempt(failCount, lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        attemptsByUsername.remove(normalize(username));
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
