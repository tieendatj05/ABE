package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.audit.AuditLogResponse;
import com.abe.system.abe_system.entity.AuditAction;
import com.abe.system.abe_system.entity.AuditLog;
import com.abe.system.abe_system.entity.FileMetadata;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Ghi audit log cho mọi lần upload/download/xoá file - yêu cầu compliance của
 * hệ thống y tế (phải truy vết được ai truy cập/cố truy cập hồ sơ nào).
 *
 * Mọi method record* đều:
 * 1) Chạy trong transaction MỚI (REQUIRES_NEW), độc lập với transaction của
 *    request gốc. Lý do: FileService.download() chạy trong transaction
 *    readOnly=true - nếu ghi audit log ngay trong transaction đó, câu lệnh
 *    INSERT có thể bị driver/connection pool âm thầm bỏ qua vì transaction
 *    được đánh dấu chỉ-đọc.
 * 2) Nuốt (swallow) mọi exception, chỉ log cảnh báo - việc ghi audit log
 *    KHÔNG BAO GIỜ được phép làm hỏng thao tác nghiệp vụ chính (upload/
 *    download/xoá file vẫn phải thành công dù audit log ghi lỗi).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUpload(FileMetadata file, User owner) {
        safeSave(AuditAction.UPLOAD, file, owner, owner, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDownloadSuccess(FileMetadata file, User requester) {
        safeSave(AuditAction.DOWNLOAD_SUCCESS, file, file.getOwner(), requester, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDownloadDenied(FileMetadata file, User requester, Set<String> activeAttributes) {
        String detail = activeAttributes.isEmpty()
                ? "Requester không có thuộc tính nào còn hiệu lực"
                : "Thuộc tính hiện có của requester: " + String.join(", ", activeAttributes);
        safeSave(AuditAction.DOWNLOAD_DENIED, file, file.getOwner(), requester, detail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDelete(Long fileId, String fileName, String accessPolicy, User owner, User requester) {
        String detail = requester.getId().equals(owner.getId())
                ? null
                : "Xoá bởi " + requester.getRole() + " (không phải chủ sở hữu)";
        try {
            auditLogRepository.save(AuditLog.builder()
                    .action(AuditAction.DELETE)
                    .fileId(fileId)
                    .fileName(fileName)
                    .ownerId(owner.getId())
                    .ownerUsername(owner.getUsername())
                    .requesterId(requester.getId())
                    .requesterUsername(requester.getUsername())
                    .accessPolicy(accessPolicy)
                    .detail(detail)
                    .build());
        } catch (Exception ex) {
            log.warn("Không ghi được audit log DELETE cho fileId={}", fileId, ex);
        }
    }

    private void safeSave(AuditAction action, FileMetadata file, User owner, User requester, String detail) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .action(action)
                    .fileId(file.getId())
                    .fileName(file.getFileName())
                    .ownerId(owner.getId())
                    .ownerUsername(owner.getUsername())
                    .requesterId(requester.getId())
                    .requesterUsername(requester.getUsername())
                    .accessPolicy(file.getAccessPolicy())
                    .detail(detail)
                    .build());
        } catch (Exception ex) {
            log.warn("Không ghi được audit log {} cho fileId={}", action, file.getId(), ex);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listAll() {
        return auditLogRepository.findAllByOrderByOccurredAtDesc().stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listForOwner(User owner) {
        return auditLogRepository.findByOwnerIdOrderByOccurredAtDesc(owner.getId()).stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
