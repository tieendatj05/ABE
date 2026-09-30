package com.abe.system.abe_system.exception;

/**
 * Ném ra khi SHA-256 của nội dung giải mã KHÔNG khớp hash lưu lúc upload -
 * xem com.abe.system.abe_system.service.ContentIntegrityService. Khác với
 * AccessDeniedException (thiếu quyền): đây là dữ liệu KHẢ NGHI ĐÃ BỊ CAN
 * THIỆP dù người tải đã đủ quyền và giải mã kỹ thuật thành công.
 */
public class ContentIntegrityException extends RuntimeException {
    public ContentIntegrityException(String message) {
        super(message);
    }
}
