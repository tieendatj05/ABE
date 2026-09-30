package com.abe.system.abe_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity User - ánh xạ tới bảng "users".
 * Lưu ý: đặt tên bảng là "users" (số nhiều) vì "user" là từ khóa dành riêng
 * (reserved keyword) trong PostgreSQL, dùng "user" làm tên bảng sẽ gây lỗi SQL.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// equals/hashCode chỉ dựa trên "id": tránh lỗi khi so sánh 2 entity có quan hệ
// 2 chiều (bidirectional) với nhau - nếu dùng @Data mặc định sẽ đệ quy vô hạn.
@EqualsAndHashCode(of = "id")
// toString loại trừ các trường quan hệ (userAttributes, files) để tránh
// vòng lặp vô hạn: User.toString() gọi UserAttribute.toString() gọi lại User.toString()...
@ToString(exclude = {"userAttributes", "files"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // Mật khẩu đã được hash (BCrypt) trước khi lưu - xử lý ở tầng Service, không phải ở Entity.
    @NotBlank
    @Column(nullable = false)
    private String password;

    @Email
    @Column(unique = true)
    private String email;

    @Column(name = "full_name", length = 100)
    private String fullName;

    // Lưu enum dạng chuỗi ("ADMIN", "DATA_OWNER", "DATA_USER") thay vì số thứ tự (ORDINAL)
    // để dữ liệu trong DB dễ đọc và không bị lệch nếu sau này thêm/xóa giá trị enum.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // Hibernate tự động gán thời điểm tạo record khi INSERT, không cần set thủ công.
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Secret TOTP (Base32) dùng để sinh/kiểm mã 6 số Google Authenticator -
     * BẮT BUỘC với role ADMIN/DEPT_ADMIN (xem AuthService.login). Nullable vì
     * DATA_OWNER/DATA_USER không cần 2FA, và ADMIN/DEPT_ADMIN mới tạo cũng
     * chưa có secret cho tới lần đăng nhập đầu tiên (lúc đó mới sinh + bắt
     * quét QR để kích hoạt - xem twoFactorEnabled).
     */
    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    /**
     * true = đã quét QR và xác nhận mã đúng ít nhất 1 lần (2FA đã kích hoạt
     * thật sự) - false = chưa kích hoạt, lần đăng nhập tới sẽ bị bắt setup lại
     * (kể cả khi totpSecret đã có, vì secret có thể đã sinh nhưng chưa xác nhận).
     */
    @Builder.Default
    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    /**
     * Phòng ban/khoa mà user này trực thuộc. Nullable vì:
     * (1) các user tạo trước khi tính năng này ra đời (dữ liệu cũ) chưa có phòng ban,
     * (2) tài khoản ADMIN toàn cục không nhất thiết thuộc phòng ban nào.
     * DEPT_ADMIN của một phòng ban chỉ được thao tác attribute/user có cùng department này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = true)
    private Department department;

    /**
     * Danh sách các thuộc tính (Attribute) mà user này được KGC cấp.
     * mappedBy = "user": phía User KHÔNG sở hữu khóa ngoại, entity UserAttribute
     * mới là chủ sở hữu quan hệ (owning side) - khóa ngoại user_id nằm ở bảng user_attributes.
     * cascade = ALL + orphanRemoval: xóa User thì các bản ghi UserAttribute liên quan
     * cũng bị xóa theo (hợp lý vì UserAttribute không có ý nghĩa tồn tại độc lập).
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAttribute> userAttributes = new ArrayList<>();

    /**
     * Danh sách file mà user này (với vai trò Data Owner) đã upload.
     */
    @Builder.Default
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileMetadata> files = new ArrayList<>();
}
