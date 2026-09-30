-- 2FA (TOTP) bắt buộc với ADMIN/DEPT_ADMIN - xem User.java, AuthService.login.
ALTER TABLE users ADD COLUMN totp_secret VARCHAR(64);
ALTER TABLE users ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;
