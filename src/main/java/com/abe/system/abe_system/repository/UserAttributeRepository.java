package com.abe.system.abe_system.repository;

import com.abe.system.abe_system.entity.Attribute;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.entity.UserAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho UserAttribute (bảng nối user_attributes).
 * Đây là repository quan trọng nhất cho logic ABE: dùng để lấy ra tập
 * thuộc tính thực tế của 1 user, phục vụ việc so khớp với accessPolicy
 * của file khi giải mã.
 */
public interface UserAttributeRepository extends JpaRepository<UserAttribute, Long> {

    // Lấy TẤT CẢ thuộc tính (kể cả đã bị thu hồi) của 1 user - dùng cho màn hình quản lý.
    List<UserAttribute> findByUser(User user);

    // Chỉ lấy thuộc tính CÒN HIỆU LỰC (revoked = false) - đây là tập thuộc tính
    // thực sự dùng để kiểm tra user có thỏa access policy của file hay không.
    List<UserAttribute> findByUserAndRevokedFalse(User user);

    // Dùng khi KGC gán thuộc tính mới: kiểm tra xem user đã có sẵn attribute này chưa
    // (tránh tạo bản ghi trùng, vi phạm UNIQUE constraint(user_id, attribute_id)).
    Optional<UserAttribute> findByUserAndAttribute(User user, Attribute attribute);
}
