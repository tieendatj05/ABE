package com.abe.system.abe_system.entity;

/**
 * Loại hành động được ghi vào AuditLog - phục vụ truy vết truy cập file
 * (yêu cầu compliance trong hệ thống giáo dục: phải biết ai truy cập/cố truy
 * cập tài liệu nào, khi nào - vd đề thi, bảng điểm).
 */
public enum AuditAction {
    UPLOAD,
    DOWNLOAD_SUCCESS,
    DOWNLOAD_DENIED,
    DELETE,
    // Giải mã thành công theo access policy (đủ attribute) NHƯNG SHA-256 của nội
    // dung giải mã không khớp hash gốc lưu lúc upload - xem ContentIntegrityService.
    // Cố tình tách riêng khỏi DOWNLOAD_DENIED vì đây là sự kiện nghiêm trọng hơn
    // hẳn (dữ liệu khả nghi bị can thiệp), không phải chỉ thiếu quyền.
    INTEGRITY_VIOLATION
}
