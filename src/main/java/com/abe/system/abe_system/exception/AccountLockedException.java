package com.abe.system.abe_system.exception;

/**
 * Ném ra khi tài khoản đang bị tạm khoá do đăng nhập sai quá nhiều lần liên
 * tiếp - xem {@link com.abe.system.abe_system.service.LoginAttemptService}.
 */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
