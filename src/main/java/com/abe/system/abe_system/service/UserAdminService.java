package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.UserResponse;
import com.abe.system.abe_system.dto.auth.RegisterRequest;
import com.abe.system.abe_system.entity.Department;
import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.exception.DuplicateResourceException;
import com.abe.system.abe_system.exception.ResourceNotFoundException;
import com.abe.system.abe_system.repository.DepartmentRepository;
import com.abe.system.abe_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Nghiệp vụ quản trị user dành riêng cho ADMIN toàn cục: liệt kê user (có thể
 * lọc theo phòng ban cho DEPT_ADMIN), tạo tài khoản giảng viên/sinh viên thay
 * (tài khoản do nhà trường cấp - không qua form tự đăng ký công khai), và
 * phong 1 user hiện có thành DEPT_ADMIN của 1 phòng ban - mô hình ABE phi tập
 * trung hóa (mỗi phòng ban là 1 KGC nhỏ).
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    // ADMIN xem tất cả; DEPT_ADMIN chỉ xem user cùng phòng ban với mình.
    @Transactional(readOnly = true)
    public List<UserResponse> listVisibleTo(User caller) {
        List<User> users = caller.getRole() == Role.ADMIN
                ? userRepository.findAll()
                : userRepository.findByDepartmentId(requireOwnDepartment(caller).getId());
        return users.stream().map(UserResponse::from).toList();
    }

    // ADMIN tạo thay tài khoản giảng viên/sinh viên (username+password do
    // ADMIN đặt sẵn) - khác với /api/auth/register (tự đăng ký công khai, chỉ
    // cho DATA_USER). Không cho tạo ADMIN/DEPT_ADMIN qua đây.
    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        if (request.getRole() != Role.DATA_OWNER && request.getRole() != Role.DATA_USER) {
            throw new IllegalArgumentException("Chỉ tạo được tài khoản DATA_OWNER hoặc DATA_USER qua chức năng này");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username đã tồn tại: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email đã được sử dụng: " + request.getEmail());
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
        return UserResponse.from(userRepository.save(user));
    }

    // Đặt role=DEPT_ADMIN và department cùng lúc (atomic) - tránh trạng thái
    // không hợp lệ "DEPT_ADMIN nhưng chưa có phòng ban".
    @Transactional
    public UserResponse promoteToDeptAdmin(Long userId, Long departmentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user id=" + userId));
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ban id=" + departmentId));
        user.setRole(Role.DEPT_ADMIN);
        user.setDepartment(department);
        return UserResponse.from(userRepository.save(user));
    }

    private Department requireOwnDepartment(User caller) {
        if (caller.getDepartment() == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Tài khoản DEPT_ADMIN chưa được gán khoa/phòng ban");
        }
        return caller.getDepartment();
    }
}
