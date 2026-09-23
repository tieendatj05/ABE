package com.abe.system.abe_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity FileMetadata - ánh xạ tới bảng "file_metadata".
 *
 * Lưu ý quan trọng: entity này KHÔNG lưu nội dung file (nội dung file đã mã
 * hóa AES-256 được lưu riêng trên ổ đĩa/storage, đường dẫn ghi ở "filePath").
 * Đây chỉ là bảng metadata mô tả:
 *   1) File nằm ở đâu (filePath) và tên gốc là gì (fileName).
 *   2) Ai được phép giải mã file (accessPolicy - biểu thức thuộc tính dạng
 *      AND/OR, ví dụ: "(department:CNTT AND position:giang_vien) OR role:ADMIN").
 *   3) Khóa AES dùng để mã hóa nội dung file, nhưng bản thân khóa AES này lại
 *      được bảo vệ theo accessPolicy ở trên (dùng Shamir Secret Sharing) rồi
 *      mới lưu vào encryptedAesKey - nên chỉ ai đủ thuộc tính thỏa chính sách
 *      mới khôi phục lại được khóa AES gốc để giải mã file.
 */
@Entity
@Table(name = "file_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "owner")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên file gốc do người dùng upload, ví dụ "bao_cao_tot_nghiep.pdf".
    @NotBlank
    @Column(name = "file_name", nullable = false)
    private String fileName;

    // Đường dẫn thực tế trên storage tới file NỘI DUNG ĐÃ MÃ HÓA (ciphertext),
    // ví dụ "storage/2026/05/uuid_bao_cao.pdf.enc". Không trùng với fileName
    // vì tên lưu trên đĩa cần là định danh duy nhất (UUID) để tránh đụng độ.
    @NotBlank
    @Column(name = "file_path", nullable = false)
    private String filePath;

    // Biểu thức chính sách truy cập dạng AND/OR trên các attributeName,
    // ví dụ: "(department:CNTT AND position:giang_vien) OR role:ADMIN".
    // Dùng TEXT vì biểu thức có thể dài, không giới hạn như VARCHAR thường.
    @Lob
    @Column(name = "access_policy", nullable = false, columnDefinition = "TEXT")
    private String accessPolicy;

    // Khóa AES-256 SAU KHI đã được mã hóa/chia sẻ theo accessPolicy bằng
    // Shamir Secret Sharing (thường là chuỗi JSON chứa các "share" gắn với
    // từng attribute lá trong policy). Không bao giờ lưu khóa AES ở dạng
    // plaintext.
    @Lob
    @Column(name = "encrypted_aes_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedAesKey;

    // Data Owner đã upload file này.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // Kích thước file gốc (byte) - phục vụ hiển thị danh sách file, không dùng cho mã hóa.
    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
