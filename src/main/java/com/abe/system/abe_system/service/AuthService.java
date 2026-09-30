package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.auth.AuthResponse;
import com.abe.system.abe_system.dto.auth.ChangePasswordRequest;
import com.abe.system.abe_system.dto.auth.LoginRequest;
import com.abe.system.abe_system.dto.auth.RegisterRequest;
import com.abe.system.abe_system.dto.auth.TwoFactorVerifyRequest;
import com.abe.system.abe_system.entity.Department;
import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.exception.DuplicateResourceException;
import com.abe.system.abe_system.exception.ResourceNotFoundException;
import com.abe.system.abe_system.repository.DepartmentRepository;
import com.abe.system.abe_system.repository.UserRepository;
import com.abe.system.abe_system.security.JwtService;
import com.abe.system.abe_system.twofactor.PendingTwoFactorService;
import com.abe.system.abe_system.twofactor.TotpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    // 2 role bắt buộc 2FA - xem lớp javadoc ở login().
    private static final Set<Role> TWO_FACTOR_REQUIRED_ROLES = Set.of(Role.ADMIN, Role.DEPT_ADMIN);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final TotpService totpService;
    private final PendingTwoFactorService pendingTwoFactorService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username đã tồn tại: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email đã được sử dụng: " + request.getEmail());
        }
        // Đăng ký công khai (không cần đăng nhập) CHỈ dành cho sinh viên
        // (DATA_USER) - tự phục vụ. Tài khoản giảng viên (DATA_OWNER) do nhà
        // trường cấp, phải được ADMIN tạo qua POST /api/users (xem
        // UserAdminService.createUser); ADMIN/DEPT_ADMIN lại càng không được
        // tự đăng ký (ADMIN: seed sẵn trong DB; DEPT_ADMIN: qua
        // POST /api/users/promote-dept-admin).
        if (request.getRole() != Role.DATA_USER) {
            throw new IllegalArgumentException("Đăng ký công khai chỉ dành cho vai trò Sinh viên (DATA_USER)");
        }

        Department department = request.getDepartmentId() == null ? null
                : departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Không tìm thấy phòng ban id=" + request.getDepartmentId()));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(request.getRole())
                .department(department)
                .build();
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    /**
     * ADMIN/DEPT_ADMIN bắt buộc 2FA (TOTP): sau khi password đúng, KHÔNG cấp
     * JWT ngay mà trả về 1 "challenge" - hoặc SETUP_REQUIRED (lần đầu, kèm QR
     * để quét) hoặc CODE_REQUIRED (đã kích hoạt, chỉ cần nhập mã 6 số). JWT
     * chỉ được cấp thật sự ở {@link #confirmTwoFactorSetup} /
     * {@link #verifyTwoFactor} sau khi mã đúng. DATA_OWNER/DATA_USER không bị
     * ảnh hưởng - đăng nhập 1 bước như cũ.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        loginAttemptService.assertNotLocked(request.getUsername());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            loginAttemptService.recordFailure(request.getUsername());
            throw ex;
        }
        loginAttemptService.recordSuccess(request.getUsername());

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User đã xác thực nhưng không tìm thấy trong DB"));

        if (!TWO_FACTOR_REQUIRED_ROLES.contains(user.getRole())) {
            return buildAuthResponse(user);
        }

        String ticket = pendingTwoFactorService.issueTicket(user.getUsername());

        if (user.isTwoFactorEnabled()) {
            return AuthResponse.builder()
                    .twoFactorChallenge("CODE_REQUIRED")
                    .twoFactorTicket(ticket)
                    .build();
        }

        // Lần đầu đăng nhập với role bắt buộc 2FA: TÁI SỬ DỤNG secret đang chờ xác
        // nhận nếu đã có (vd người dùng bấm đăng nhập 2 lần, hoặc F5 lại trang
        // trước khi kịp quét/nhập mã) - chỉ sinh secret MỚI khi thật sự chưa từng
        // có. Trước đây sinh secret mới ở MỖI lần gọi login() khi chưa xác nhận,
        // khiến QR vừa hiển thị trên màn hình bị đổi ngầm nếu có lần gọi login()
        // thứ 2 xen vào trước khi người dùng kịp nhập mã - mã đúng với QR cũ vẫn
        // báo sai vì server đã chuyển sang secret khác.
        String secret = user.getTotpSecret();
        if (secret == null) {
            secret = totpService.generateSecret();
            user.setTotpSecret(secret);
            userRepository.save(user);
        }

        return AuthResponse.builder()
                .twoFactorChallenge("SETUP_REQUIRED")
                .twoFactorTicket(ticket)
                .qrCodeDataUri(totpService.generateQrCodeDataUri(user.getUsername(), secret))
                .manualEntryKey(secret)
                .build();
    }

    /**
     * Bước 2 khi lần đầu kích hoạt 2FA: xác nhận mã 6 số khớp với secret vừa
     * sinh ở {@link #login}, rồi mới thật sự bật {@code twoFactorEnabled} và
     * cấp JWT. Nếu chưa xác nhận đúng 1 lần, secret coi như chưa kích hoạt -
     * lần login sau vẫn bị bắt setup lại (không tự tin dùng 1 secret chưa ai
     * xác nhận là của đúng người).
     */
    @Transactional
    public AuthResponse confirmTwoFactorSetup(TwoFactorVerifyRequest request) {
        User user = resolveByTicketOrThrow(request.getTicket());

        if (user.getTotpSecret() == null || !totpService.verifyCode(user.getTotpSecret(), request.getCode())) {
            throw new IllegalArgumentException("Mã xác thực không đúng, vui lòng thử lại");
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        pendingTwoFactorService.consume(request.getTicket());
        return buildAuthResponse(user);
    }

    /**
     * Bước 2 cho lần login sau khi 2FA đã kích hoạt: chỉ kiểm mã, không đổi
     * gì thêm.
     */
    public AuthResponse verifyTwoFactor(TwoFactorVerifyRequest request) {
        User user = resolveByTicketOrThrow(request.getTicket());

        if (!user.isTwoFactorEnabled() || user.getTotpSecret() == null
                || !totpService.verifyCode(user.getTotpSecret(), request.getCode())) {
            throw new IllegalArgumentException("Mã xác thực không đúng, vui lòng thử lại");
        }

        pendingTwoFactorService.consume(request.getTicket());
        return buildAuthResponse(user);
    }

    private User resolveByTicketOrThrow(String ticket) {
        String username = pendingTwoFactorService.resolve(ticket)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vé xác thực không hợp lệ hoặc đã hết hạn, vui lòng đăng nhập lại"));
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + username));
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .authorities(java.util.List.of())
                        .build());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .build();
    }
}
