package com.abe.system.abe_system.service;

import com.abe.system.abe_system.exception.ContentIntegrityException;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Kiểm tra toàn vẹn nội dung file bằng SHA-256: hash file GỐC lúc upload,
 * hash lại nội dung ĐÃ GIẢI MÃ lúc download rồi so khớp. Đây là lớp bảo vệ
 * BỔ SUNG, độc lập với việc AES-256/GCM (AEAD) vốn đã tự phát hiện ciphertext
 * bị sửa (GCM tag sai sẽ làm decrypt() ném lỗi trước khi tới được bước này) -
 * SHA-256 ở đây xác nhận một điều RỘNG HƠN: nội dung nhận về sau TOÀN BỘ
 * pipeline (mã hoá -> lưu trữ -> truy xuất -> giải mã) đúng bit-for-bit với
 * file gốc, kể cả nếu nguyên nhân sai khác không phải do ciphertext bị sửa
 * (vd nhầm lẫn ở tầng lưu trữ, lỗi phần mềm) - và tạo ra 1 bằng chứng hash rõ
 * ràng, dễ giải thích khi audit thay vì chỉ dựa vào "AEAD không throw lỗi".
 */
@Service
public class ContentIntegrityService {

    public String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luon co san tren moi JVM chuan - khong bao gio xay ra thuc te.
            throw new IllegalStateException("Thuật toán SHA-256 không khả dụng trên JVM này", e);
        }
    }

    /**
     * @param expectedHash hash lưu lúc upload (FileMetadata.contentHash) - null
     *                      nghĩa là file upload trước khi có tính năng này, bỏ
     *                      qua kiểm tra thay vì báo lỗi oan.
     * @param actualContent nội dung VỪA GIẢI MÃ, sẽ được hash lại để so sánh.
     */
    public void verify(String expectedHash, byte[] actualContent) {
        if (expectedHash == null) {
            return;
        }
        String actualHash = sha256Hex(actualContent);
        if (!expectedHash.equalsIgnoreCase(actualHash)) {
            throw new ContentIntegrityException("Tài liệu đã bị can thiệp");
        }
    }
}
