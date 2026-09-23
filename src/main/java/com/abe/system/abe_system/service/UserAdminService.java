package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.UserResponse;
import com.abe.system.abe_system.entity.Department;
import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.exception.ResourceNotFoundException;
import com.abe.system.abe_system.repository.DepartmentRepository;
import com.abe.system.abe_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Nghiệp vụ quản trị user dành riêng cho ADMIN toàn cục: liệt kê user (có thể
 * lọc theo phòng ban cho DEPT_ADMIN) và phong 1 user hiện có thành DEPT_ADMIN
 * của 1 phòng ban - mô hình ABE phi tập trung hóa (mỗi phòng ban là 1 KGC nhỏ).
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    // ADMIN xem tất cả; DEPT_ADMIN chỉ xem user cùng phòng ban với mình.
    @Transactional(readOnly = true)
    public List<UserResponse> listVisibleTo(User caller) {
        List<User> users = caller.getRole() == Role.ADMIN
                ? userRepository.findAll()
                : userRepository.findByDepartmentId(requireOwnDepartment(caller).getId());
        return users.stream().map(UserResponse::from).toList();
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
