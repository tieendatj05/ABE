package com.abe.system.abe_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity UserAttribute - ánh xạ tới bảng trung gian "user_attributes".
 *
 * Đây KHÔNG phải là quan hệ @ManyToMany thuần túy, mà là một "association
 * entity" (entity đại diện cho bảng nối): mỗi bản ghi UserAttribute thể hiện
 * việc "user X được KGC cấp attribute Y", kèm theo metadata riêng
 * (assignedAt, revoked). Nhờ vậy sau này Admin/KGC có thể thu hồi
 * (revoke) một thuộc tính cụ thể của một user mà không ảnh hưởng tới các
 * thuộc tính khác - điều mà @ManyToMany thuần không làm được vì bảng nối
 * lúc đó chỉ có đúng 2 cột khóa ngoại, không có chỗ lưu thêm dữ liệu.
 *
 * Quan hệ 2 chiều với User và Attribute đều dùng @ManyToOne vì UserAttribute
 * là "phía nhiều" nhìn từ cả hai bảng kia (1 user có nhiều UserAttribute,
 * 1 attribute cũng có nhiều UserAttribute).
 */
@Entity
@Table(
    name = "user_attributes",
    // Ràng buộc UNIQUE (user_id, attribute_id): đảm bảo 1 user không thể
    // được gán trùng lặp cùng 1 attribute nhiều lần.
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "attribute_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "attribute", "issuedBy"})
public class UserAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FetchType.LAZY: khi load 1 UserAttribute, KHÔNG tự động load kèm User
     * đầy đủ - chỉ load khi thực sự truy cập user.getXxx(). Tránh N+1 query
     * và tránh load dư thừa dữ liệu không cần thiết.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    // Thời điểm KGC cấp thuộc tính này cho user.
    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    // Cờ đánh dấu thuộc tính đã bị Admin/KGC thu hồi hay chưa.
    // Khi giải mã, hệ thống phải kiểm tra cờ này thay vì chỉ dựa vào
    // sự tồn tại của bản ghi (để giữ lại lịch sử cấp/thu hồi thay vì xóa cứng).
    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    /**
     * Ai (ADMIN hoặc DEPT_ADMIN) đã thực hiện lần cấp/tái cấp (re-grant) gần nhất
     * cho bản ghi này - phục vụ truy vết trách nhiệm (accountability) trong mô hình
     * đa authority. Nullable vì các bản ghi tạo trước khi có tính năng này (dữ liệu cũ)
     * không có thông tin này - hiển thị "Không rõ" ở giao diện thay vì lỗi.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_user_id", nullable = true)
    private User issuedBy;
}
