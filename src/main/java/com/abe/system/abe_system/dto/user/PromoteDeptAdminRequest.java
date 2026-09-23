package com.abe.system.abe_system.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload để ADMIN phong 1 user hiện có thành DEPT_ADMIN của 1 phòng ban
 * (POST /api/users/promote-dept-admin). role và department được set đồng thời
 * (atomic) để tránh trạng thái không hợp lệ "DEPT_ADMIN nhưng không có phòng ban".
 */
@Getter
@Setter
public class PromoteDeptAdminRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long departmentId;
}
