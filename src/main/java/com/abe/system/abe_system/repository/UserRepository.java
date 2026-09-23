package com.abe.system.abe_system.repository;

import com.abe.system.abe_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho User - Spring Data JPA tự sinh implementation dựa trên
 * tên method (query derivation), không cần viết SQL/JPQL thủ công cho
 * các thao tác đơn giản như dưới đây.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // Dùng khi đăng nhập: tìm user theo username để lấy password (hash) và role.
    Optional<User> findByUsername(String username);

    // Dùng khi đăng ký: kiểm tra username đã tồn tại chưa trước khi tạo user mới.
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Dùng cho DEPT_ADMIN xem danh sách nhân viên của phòng ban mình.
    List<User> findByDepartmentId(Long departmentId);

    boolean existsByDepartmentId(Long departmentId);
}
