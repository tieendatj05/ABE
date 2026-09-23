package com.abe.system.abe_system.controller;

import com.abe.system.abe_system.dto.audit.AuditLogResponse;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.exception.ResourceNotFoundException;
import com.abe.system.abe_system.repository.UserRepository;
import com.abe.system.abe_system.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API xem audit log (nhật ký truy cập file). GET / chỉ ADMIN (xem toàn bộ hệ
 * thống, xem SecurityConfig); GET /mine cho phép mọi user đã đăng nhập xem
 * lịch sử truy cập của các file MÌNH SỞ HỮU (Data Owner theo dõi ai đã/đang
 * cố truy cập hồ sơ do mình quản lý).
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @GetMapping
    public List<AuditLogResponse> listAll() {
        return auditLogService.listAll();
    }

    @GetMapping("/mine")
    public List<AuditLogResponse> listMine(Authentication authentication) {
        User owner = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user: " + authentication.getName()));
        return auditLogService.listForOwner(owner);
    }
}
