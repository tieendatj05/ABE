package com.abe.system.abe_system.controller;

import com.abe.system.abe_system.dto.attribute.AssignAttributeRequest;
import com.abe.system.abe_system.dto.attribute.AttributeRequest;
import com.abe.system.abe_system.dto.attribute.AttributeResponse;
import com.abe.system.abe_system.dto.attribute.UserAttributeResponse;
import com.abe.system.abe_system.service.AttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API quản lý thuộc tính - ADMIN hoặc DEPT_ADMIN mới gọi được (chặn ở
 * SecurityConfig, requestMatchers("/api/attributes/**").hasAnyRole("ADMIN","DEPT_ADMIN")).
 * Phạm vi cụ thể (ADMIN toàn quyền, DEPT_ADMIN chỉ trong phòng ban của mình)
 * được kiểm tra ở AttributeService dựa trên caller lấy từ Authentication.
 */
@RestController
@RequestMapping("/api/attributes")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    @PostMapping
    public ResponseEntity<AttributeResponse> create(@Valid @RequestBody AttributeRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attributeService.create(request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<AttributeResponse>> listAll(Authentication authentication) {
        return ResponseEntity.ok(attributeService.listAll(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        attributeService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assign")
    public ResponseEntity<UserAttributeResponse> assign(@Valid @RequestBody AssignAttributeRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(attributeService.assign(
                request.getUserId(), request.getAttributeId(), authentication.getName()));
    }

    @PostMapping("/revoke/{userAttributeId}")
    public ResponseEntity<Void> revoke(@PathVariable Long userAttributeId, Authentication authentication) {
        attributeService.revoke(userAttributeId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserAttributeResponse>> listByUser(@PathVariable Long userId,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(attributeService.listByUser(userId, authentication.getName()));
    }

    /**
     * Cho phép CHÍNH user đang đăng nhập (bất kể role) xem attribute (còn hiệu
     * lực) của mình - khác với /user/{userId} ở trên (chỉ ADMIN gọi được).
     * Dùng để frontend vẽ trực quan cây chính sách AND/OR: attribute nào user
     * đang có sẽ hiện dấu tích, thiếu thì hiện dấu X - không lộ attribute của
     * người khác.
     */
    @GetMapping("/me")
    public ResponseEntity<List<UserAttributeResponse>> listMine(Authentication authentication) {
        return ResponseEntity.ok(attributeService.listMine(authentication.getName()));
    }
}
