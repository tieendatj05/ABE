package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.auth.AuthResponse;
import com.abe.system.abe_system.dto.auth.ChangePasswordRequest;
import com.abe.system.abe_system.dto.auth.LoginRequest;
import com.abe.system.abe_system.dto.auth.RegisterRequest;
import com.abe.system.abe_system.dto.auth.TwoFactorVerifyRequest;
import com.abe.system.abe_system.entity.Department;
import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.exception.AccountLockedException;
import com.abe.system.abe_system.repository.DepartmentRepository;
import com.abe.system.abe_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void selfRegisteringAsDeptAdminIsRejected() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("would_be_dept_admin");
        request.setPassword("secret123");
        request.setEmail("would_be_dept_admin@test.com");
        request.setFullName("Nope");
        request.setRole(Role.DEPT_ADMIN);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registeringWithDepartmentIdAttachesDepartment() {
        Department department = departmentRepository.save(
                Department.builder().name("Khoa Test Auth").code("AUTH_TEST").build());

        RegisterRequest request = new RegisterRequest();
        request.setUsername("student_with_dept");
        request.setPassword("secret123");
        request.setEmail("student_with_dept@test.com");
        request.setFullName("Student With Dept");
        request.setRole(Role.DATA_USER);
        request.setDepartmentId(department.getId());

        AuthResponse response = authService.register(request);
        assertThat(response.getDepartmentId()).isEqualTo(department.getId());
        assertThat(response.getDepartmentName()).isEqualTo("Khoa Test Auth");
    }

    // Đăng ký công khai giờ chỉ dành cho sinh viên (DATA_USER) - tài khoản
    // giảng viên (DATA_OWNER) do ADMIN tạo qua UserAdminService.createUser().
    @Test
    void selfRegisteringAsDataOwnerIsRejected() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("would_be_teacher");
        request.setPassword("secret123");
        request.setEmail("would_be_teacher@test.com");
        request.setFullName("Nope");
        request.setRole(Role.DATA_OWNER);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accountGetsLockedAfterTooManyFailedLoginAttempts() {
        userRepository.save(User.builder()
                .username("lockout_test_user")
                .password(passwordEncoder.encode("CorrectPass123!"))
                .email("lockout_test_user@test.com")
                .fullName("Lockout Test")
                .role(Role.DATA_USER)
                .build());

        LoginRequest wrongPassword = new LoginRequest();
        wrongPassword.setUsername("lockout_test_user");
        wrongPassword.setPassword("wrong_password");

        // Mac dinh security.login.max-attempts=5 - sai du 5 lan se bi khoa.
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(wrongPassword))
                    .isInstanceOf(BadCredentialsException.class);
        }

        LoginRequest correctPassword = new LoginRequest();
        correctPassword.setUsername("lockout_test_user");
        correctPassword.setPassword("CorrectPass123!");

        // Sau khi bi khoa, du go dung mat khau cung bi tu choi cho den khi het thoi gian khoa.
        assertThatThrownBy(() -> authService.login(correctPassword))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void changePasswordUpdatesHashAndRejectsWrongCurrentPassword() {
        User user = userRepository.save(User.builder()
                .username("change_pw_user")
                .password(passwordEncoder.encode("OldPass123!"))
                .email("change_pw_user@test.com")
                .fullName("Change Pw")
                .role(Role.DATA_USER)
                .build());

        ChangePasswordRequest wrongCurrent = new ChangePasswordRequest();
        wrongCurrent.setCurrentPassword("NotTheRealOne1!");
        wrongCurrent.setNewPassword("NewPass123!");
        assertThatThrownBy(() -> authService.changePassword("change_pw_user", wrongCurrent))
                .isInstanceOf(IllegalArgumentException.class);

        ChangePasswordRequest correct = new ChangePasswordRequest();
        correct.setCurrentPassword("OldPass123!");
        correct.setNewPassword("NewPass123!");
        authService.changePassword("change_pw_user", correct);

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewPass123!", reloaded.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("OldPass123!", reloaded.getPassword())).isFalse();
    }

    @Test
    void adminLoginRequiresTwoFactorSetupOnFirstLogin() {
        userRepository.save(User.builder()
                .username("admin_2fa_setup")
                .password(passwordEncoder.encode("AdminPass123!"))
                .email("admin_2fa_setup@test.com")
                .fullName("Admin 2FA")
                .role(Role.ADMIN)
                .build());

        LoginRequest login = new LoginRequest();
        login.setUsername("admin_2fa_setup");
        login.setPassword("AdminPass123!");

        AuthResponse response = authService.login(login);

        assertThat(response.getToken()).isNull();
        assertThat(response.getTwoFactorChallenge()).isEqualTo("SETUP_REQUIRED");
        assertThat(response.getTwoFactorTicket()).isNotBlank();
        assertThat(response.getQrCodeDataUri()).startsWith("data:image/png;base64,");
        assertThat(response.getManualEntryKey()).isNotBlank();
    }

    @Test
    void adminCompletesTwoFactorSetupAndGetsToken() throws Exception {
        userRepository.save(User.builder()
                .username("admin_2fa_confirm")
                .password(passwordEncoder.encode("AdminPass123!"))
                .email("admin_2fa_confirm@test.com")
                .fullName("Admin 2FA Confirm")
                .role(Role.ADMIN)
                .build());

        LoginRequest login = new LoginRequest();
        login.setUsername("admin_2fa_confirm");
        login.setPassword("AdminPass123!");
        AuthResponse challenge = authService.login(login);

        TwoFactorVerifyRequest confirm = new TwoFactorVerifyRequest();
        confirm.setTicket(challenge.getTwoFactorTicket());
        confirm.setCode(currentTotpCode(challenge.getManualEntryKey()));

        AuthResponse result = authService.confirmTwoFactorSetup(confirm);

        assertThat(result.getToken()).isNotBlank();
        User reloaded = userRepository.findByUsername("admin_2fa_confirm").orElseThrow();
        assertThat(reloaded.isTwoFactorEnabled()).isTrue();
    }

    // Bug thực tế phát hiện qua smoke test: trước đây login() sinh secret MỚI ở
    // MỖI lần gọi khi chưa xác nhận 2FA - nếu người dùng bấm đăng nhập 2 lần
    // (hoặc F5 lại trang) trước khi kịp quét/nhập mã, QR đang hiển thị bị đổi
    // ngầm và mã đúng với QR cũ luôn báo sai. Phải tái sử dụng secret đang chờ
    // xác nhận thay vì sinh mới mỗi lần.
    @Test
    void repeatedLoginBeforeConfirmingSetupReusesSameSecret() {
        userRepository.save(User.builder()
                .username("admin_2fa_repeat_login")
                .password(passwordEncoder.encode("AdminPass123!"))
                .email("admin_2fa_repeat_login@test.com")
                .fullName("Admin Repeat Login")
                .role(Role.ADMIN)
                .build());

        LoginRequest login = new LoginRequest();
        login.setUsername("admin_2fa_repeat_login");
        login.setPassword("AdminPass123!");

        AuthResponse first = authService.login(login);
        AuthResponse second = authService.login(login);

        assertThat(second.getTwoFactorChallenge()).isEqualTo("SETUP_REQUIRED");
        assertThat(second.getManualEntryKey()).isEqualTo(first.getManualEntryKey());
    }

    @Test
    void wrongTotpCodeIsRejectedDuringSetup() {
        userRepository.save(User.builder()
                .username("admin_2fa_wrong_code")
                .password(passwordEncoder.encode("AdminPass123!"))
                .email("admin_2fa_wrong_code@test.com")
                .fullName("Admin Wrong Code")
                .role(Role.ADMIN)
                .build());

        LoginRequest login = new LoginRequest();
        login.setUsername("admin_2fa_wrong_code");
        login.setPassword("AdminPass123!");
        AuthResponse challenge = authService.login(login);

        TwoFactorVerifyRequest confirm = new TwoFactorVerifyRequest();
        confirm.setTicket(challenge.getTwoFactorTicket());
        confirm.setCode("000000");

        assertThatThrownBy(() -> authService.confirmTwoFactorSetup(confirm))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adminWithTwoFactorAlreadyEnabledMustVerifyCodeOnLogin() throws Exception {
        User admin = userRepository.save(User.builder()
                .username("admin_2fa_enabled")
                .password(passwordEncoder.encode("AdminPass123!"))
                .email("admin_2fa_enabled@test.com")
                .fullName("Admin 2FA Enabled")
                .role(Role.ADMIN)
                .totpSecret(new dev.samstevens.totp.secret.DefaultSecretGenerator().generate())
                .twoFactorEnabled(true)
                .build());

        LoginRequest login = new LoginRequest();
        login.setUsername("admin_2fa_enabled");
        login.setPassword("AdminPass123!");
        AuthResponse challenge = authService.login(login);

        assertThat(challenge.getToken()).isNull();
        assertThat(challenge.getTwoFactorChallenge()).isEqualTo("CODE_REQUIRED");
        // Da kich hoat tu truoc thi KHONG duoc gui lai QR/secret nua.
        assertThat(challenge.getQrCodeDataUri()).isNull();

        TwoFactorVerifyRequest verify = new TwoFactorVerifyRequest();
        verify.setTicket(challenge.getTwoFactorTicket());
        verify.setCode(currentTotpCode(admin.getTotpSecret()));

        AuthResponse result = authService.verifyTwoFactor(verify);
        assertThat(result.getToken()).isNotBlank();
    }

    @Test
    void expiredOrUnknownTicketIsRejected() {
        TwoFactorVerifyRequest request = new TwoFactorVerifyRequest();
        request.setTicket("nonexistent-ticket");
        request.setCode("123456");

        assertThatThrownBy(() -> authService.verifyTwoFactor(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dataOwnerLoginIsUnaffectedByTwoFactor() {
        userRepository.save(User.builder()
                .username("owner_no_2fa")
                .password(passwordEncoder.encode("OwnerPass123!"))
                .email("owner_no_2fa@test.com")
                .fullName("Owner No 2FA")
                .role(Role.DATA_OWNER)
                .build());

        LoginRequest login = new LoginRequest();
        login.setUsername("owner_no_2fa");
        login.setPassword("OwnerPass123!");

        AuthResponse response = authService.login(login);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getTwoFactorChallenge()).isNull();
    }

    private String currentTotpCode(String secret) throws Exception {
        dev.samstevens.totp.code.CodeGenerator codeGenerator = new dev.samstevens.totp.code.DefaultCodeGenerator();
        dev.samstevens.totp.time.TimeProvider timeProvider = new dev.samstevens.totp.time.SystemTimeProvider();
        return codeGenerator.generate(secret, timeProvider.getTime() / 30);
    }
}
