package com.abe.system.abe_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity Department - ánh xạ tới bảng "departments".
 *
 * Đại diện cho một khoa/bộ môn trong trường học (vd "Khoa CNTT", mã "CNTT").
 * Đây là nền tảng cho mô hình ABE phi tập trung hóa: mỗi Department có thể có
 * một User giữ vai trò DEPT_ADMIN (KGC riêng của phòng ban đó), tự quản lý tập
 * thuộc tính (Attribute.issuerDepartment) và user (User.department) của mình,
 * thay vì chỉ một ADMIN toàn cục quản lý tất cả.
 *
 * Không khai báo quan hệ @OneToMany ngược lại User/Attribute ở đây - tra cứu
 * qua repository (findByDepartmentId) là đủ, tránh nguy cơ đệ quy equals/toString
 * hai chiều như đã tránh ở các entity khác trong dự án.
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên đầy đủ, hiển thị trên giao diện, vd "Khoa CNTT".
    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // Mã ngắn gọn dùng làm quy ước đặt tên attribute, vd "NOI", "XETNGHIEM".
    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
