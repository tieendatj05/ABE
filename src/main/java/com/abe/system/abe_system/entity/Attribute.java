package com.abe.system.abe_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity Attribute - ánh xạ tới bảng "attributes".
 * Đây là "kho" thuộc tính do Admin/KGC định nghĩa, ví dụ: "department:CNTT",
 * "position:giang_vien", "clearance:top_secret"... Các thuộc tính này sau đó
 * được: (1) gán cho User qua UserAttribute, và (2) dùng trong access policy
 * của FileMetadata (ví dụ: "department:CNTT AND position:giang_vien").
 */
@Entity
@Table(name = "attributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "userAttributes")
public class Attribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên thuộc tính phải là duy nhất, ví dụ "department:CNTT".
    // Đây chính là "nhãn" (label) được dùng trực tiếp trong biểu thức chính sách
    // truy cập ở FileMetadata.accessPolicy.
    @NotBlank
    @Column(name = "attribute_name", nullable = false, unique = true, length = 100)
    private String attributeName;

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Phòng ban đã tạo (issue) attribute này - null nghĩa là attribute "toàn cục" do
     * ADMIN tạo (vd "role:ADMIN"). Khác null nghĩa là attribute do DEPT_ADMIN của
     * phòng ban đó tạo, và chỉ DEPT_ADMIN cùng phòng ban mới được sửa/xóa/gán/thu hồi
     * attribute này (mô hình ABE phi tập trung hóa - mỗi phòng ban là 1 authority riêng).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_department_id", nullable = true)
    private Department issuerDepartment;

    /**
     * Danh sách các user đang sở hữu thuộc tính này (chiều ngược lại của quan hệ).
     * mappedBy = "attribute" trỏ tới field "attribute" bên entity UserAttribute.
     */
    @Builder.Default
    @OneToMany(mappedBy = "attribute", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAttribute> userAttributes = new ArrayList<>();
}
