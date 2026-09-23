package com.abe.system.abe_system.entity;

/**
 * Loại hành động được ghi vào AuditLog - phục vụ truy vết truy cập file
 * (yêu cầu compliance trong hệ thống y tế: phải biết ai truy cập/cố truy cập
 * hồ sơ nào, khi nào).
 */
public enum AuditAction {
    UPLOAD,
    DOWNLOAD_SUCCESS,
    DOWNLOAD_DENIED,
    DELETE
}
