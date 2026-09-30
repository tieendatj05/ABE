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
 * hóa AES-256 được lưu ở object storage - xem package {@code storage},
 * "storageKey" là object key/tên đối tượng trên đó, KHÔNG phải đường dẫn ổ
 * đĩa local nữa kể từ khi chuyển sang MinIO).
 * Đây chỉ là bảng metadata mô tả:
 *   1) File nằm ở đâu (storageKey) và tên gốc là gì (fileName).
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

    // Object key trên object storage (MinIO) tới file NỘI DUNG ĐÃ MÃ HÓA
    // (ciphertext), ví dụ "3f2a1e-uuid.enc". Không trùng với fileName vì key
    // lưu trữ cần là định danh duy nhất (UUID) để tránh đụng độ. Cột DB vẫn
    // tên "file_path" (giữ nguyên từ trước khi có MinIO) để khỏi cần thêm
    // migration đổi tên cột - chỉ đổi tên/ý nghĩa ở tầng Java cho rõ ràng.
    @NotBlank
    @Column(name = "file_path", nullable = false)
    private String storageKey;

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

    /**
     * SHA-256 (hex, 64 ký tự) của nội dung file GỐC (plaintext), tính lúc
     * upload - dùng để phát hiện can thiệp: sau khi giải mã lúc download, hệ
     * thống hash lại và so khớp (xem ContentIntegrityService). Nullable vì
     * các file upload TRƯỚC khi có tính năng này chưa có hash - download vẫn
     * cho qua bình thường (bỏ qua bước kiểm tra) thay vì báo lỗi oan.
     */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
