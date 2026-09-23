package com.abe.system.abe_system.entity;

/**
 * 4 vai trò trong hệ thống ABE.
 * - ADMIN: Admin/KGC toàn cục (Key Generation Center) - quản lý phòng ban, thuộc tính toàn cục,
 *   cấp/thu hồi thuộc tính cho user, và phong DEPT_ADMIN cho từng phòng ban.
 * - DEPT_ADMIN: KGC cấp phòng ban (mô hình ABE phi tập trung hóa) - chỉ được tạo/xóa thuộc tính
 *   thuộc phòng ban của chính mình, và chỉ được gán/thu hồi thuộc tính đó cho user cùng phòng ban.
 * - DATA_OWNER: chủ sở hữu dữ liệu - upload và mã hóa file theo chính sách truy cập.
 * - DATA_USER: người dùng dữ liệu - tải và giải mã file nếu thuộc tính của họ thỏa chính sách.
 */
public enum Role {
    ADMIN,
    DEPT_ADMIN,
    DATA_OWNER,
    DATA_USER
}
