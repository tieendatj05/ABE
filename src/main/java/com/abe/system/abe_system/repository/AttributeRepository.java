package com.abe.system.abe_system.repository;

import com.abe.system.abe_system.entity.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho Attribute - phục vụ Admin/KGC quản lý "kho" thuộc tính.
 */
public interface AttributeRepository extends JpaRepository<Attribute, Long> {

    Optional<Attribute> findByAttributeName(String attributeName);

    // Kiểm tra trùng tên thuộc tính trước khi Admin tạo mới (attributeName là UNIQUE).
    boolean existsByAttributeName(String attributeName);

    // Dùng cho DEPT_ADMIN: xem attribute toàn cục (issuerDepartment null) và attribute
    // của chính phòng ban mình - không thấy attribute của phòng ban khác.
    List<Attribute> findByIssuerDepartmentIsNullOrIssuerDepartmentId(Long departmentId);

    boolean existsByIssuerDepartmentId(Long departmentId);
}
