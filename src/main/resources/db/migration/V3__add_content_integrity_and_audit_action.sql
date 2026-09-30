-- Kiểm tra toàn vẹn dữ liệu (SHA-256) - xem FileMetadata.java, ContentIntegrityService.
-- Nullable: file upload TRƯỚC migration này không có hash, download vẫn cho qua
-- bình thường (bỏ qua kiểm tra) thay vì báo lỗi oan cho dữ liệu cũ.
ALTER TABLE file_metadata ADD COLUMN content_hash VARCHAR(64);
