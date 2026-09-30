package com.abe.system.abe_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity AuditLog - ánh xạ tới bảng "audit_logs".
 *
 * Ghi lại MỌI lần truy cập/cố truy cập file (upload, download thành công,
 * download bị từ chối, xoá) - đây là yêu cầu compliance đặc trưng của hệ
 * thống giáo dục (phải chứng minh được ai đã xem/cố xem tài liệu nào, khi nào -
 * vd chứng minh không ai mở đề thi trước ngày thi).
 *
 * Cố tình KHÔNG dùng @ManyToOne/FK tới FileMetadata hay User mà chỉ lưu các
 * trường "snapshot" (id + tên tại thời điểm xảy ra sự kiện): nhờ vậy log vẫn
 * còn nguyên vẹn kể cả sau khi file hoặc user bị xoá - đúng tinh thần audit
 * trail (bằng chứng phải tồn tại độc lập, không phụ thuộc dữ liệu sống).
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "owner_username", nullable = false, length = 50)
    private String ownerUsername;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "requester_username", nullable = false, length = 50)
    private String requesterUsername;

    // Snapshot accessPolicy tại thời điểm xảy ra sự kiện, phục vụ tra cứu sau này.
    @Lob
    @Column(name = "access_policy", columnDefinition = "TEXT")
    private String accessPolicy;

    // Chi tiết: vd với DOWNLOAD_DENIED là tập attribute hiện có của requester.
    @Lob
    @Column(columnDefinition = "TEXT")
    private String detail;

    @CreationTimestamp
    @Column(name = "occurred_at", updatable = false)
    private LocalDateTime occurredAt;
}
