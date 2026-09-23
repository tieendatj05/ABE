package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.auth.AuthResponse;
import com.abe.system.abe_system.dto.auth.LoginRequest;
import com.abe.system.abe_system.dto.auth.RegisterRequest;
import com.abe.system.abe_system.entity.Department;
import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.exception.DuplicateResourceException;
import com.abe.system.abe_system.exception.ResourceNotFoundException;
import com.abe.system.abe_system.repository.DepartmentRepository;
import com.abe.system.abe_system.repository.UserRepository;
import com.abe.system.abe_system.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username đã tồn tại: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email đã được sử dụng: " + request.getEmail());
        }
        // Không cho phép tự đăng ký với role ADMIN hoặc DEPT_ADMIN - cả hai đều là
        // "authority" (KGC toàn cục / KGC phòng ban) và phải được tạo bởi ADMIN
        // khác (ADMIN: seed sẵn trong DB; DEPT_ADMIN: qua POST /api/users/promote-dept-admin).
        if (request.getRole() == Role.ADMIN || request.getRole() == Role.DEPT_ADMIN) {
            throw new IllegalArgumentException("Không thể tự đăng ký tài khoản ADMIN hoặc DEPT_ADMIN");
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

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User đã xác thực nhưng không tìm thấy trong DB"));

        return buildAuthResponse(user);
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
