package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.department.DepartmentRequest;
import com.abe.system.abe_system.dto.department.DepartmentResponse;
import com.abe.system.abe_system.entity.Department;
import com.abe.system.abe_system.exception.DuplicateResourceException;
import com.abe.system.abe_system.exception.ResourceNotFoundException;
import com.abe.system.abe_system.repository.AttributeRepository;
import com.abe.system.abe_system.repository.DepartmentRepository;
import com.abe.system.abe_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quản lý Department (khoa/phòng ban) - chỉ ADMIN toàn cục được tạo/xoá
 * (xem SecurityConfig), vì Department là nền tảng để phong DEPT_ADMIN nên
 * không thể để chính DEPT_ADMIN tự tạo phòng ban cho mình.
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AttributeRepository attributeRepository;

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Phòng ban đã tồn tại: " + request.getName());
        }
        if (departmentRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Mã phòng ban đã tồn tại: " + request.getCode());
        }
        Department department = Department.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .build();
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listAll() {
        return departmentRepository.findAll().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng ban id=" + departmentId));
        // Chặn xoá nếu còn user/attribute tham chiếu tới - tránh để lộ
        // DataIntegrityViolationException (500 thô) do vi phạm khoá ngoại.
        if (userRepository.existsByDepartmentId(departmentId) || attributeRepository.existsByIssuerDepartmentId(departmentId)) {
            throw new IllegalArgumentException(
                    "Không thể xoá phòng ban đang có user hoặc attribute liên kết");
        }
        departmentRepository.delete(department);
    }
}
