package com.abe.system.abe_system.controller;

import com.abe.system.abe_system.dto.UserResponse;
import com.abe.system.abe_system.dto.auth.RegisterRequest;
import com.abe.system.abe_system.dto.user.PromoteDeptAdminRequest;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.exception.ResourceNotFoundException;
import com.abe.system.abe_system.repository.UserRepository;
import com.abe.system.abe_system.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Chỉ ADMIN/DEPT_ADMIN mới gọi được (xem SecurityConfig). GET / dùng để tra
 * cứu userId trước khi gọi POST /api/attributes/assign - ADMIN thấy tất cả,
 * DEPT_ADMIN chỉ thấy user cùng phòng ban với mình.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserAdminService userAdminService;

    @GetMapping
    public List<UserResponse> listAll(Authentication authentication) {
        User caller = currentUser(authentication);
        return userAdminService.listVisibleTo(caller);
    }

    // ADMIN tạo thay tài khoản giảng viên/sinh viên (xem SecurityConfig -
    // matcher đặt trước rule chung /api/users/** để chặn DEPT_ADMIN gọi path này).
    @PostMapping
    public UserResponse createUser(@Valid @RequestBody RegisterRequest request) {
        return userAdminService.createUser(request);
    }

    // Chỉ ADMIN toàn cục (xem SecurityConfig - matcher đặt trước rule chung /api/users/**).
    @PostMapping("/promote-dept-admin")
    public UserResponse promoteToDeptAdmin(@Valid @RequestBody PromoteDeptAdminRequest request) {
        return userAdminService.promoteToDeptAdmin(request.getUserId(), request.getDepartmentId());
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + authentication.getName()));
    }
}
