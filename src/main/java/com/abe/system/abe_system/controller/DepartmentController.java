package com.abe.system.abe_system.controller;

import com.abe.system.abe_system.dto.department.DepartmentRequest;
import com.abe.system.abe_system.dto.department.DepartmentResponse;
import com.abe.system.abe_system.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API quản lý phòng ban. GET mở public (permitAll trong SecurityConfig) vì
 * RegisterPage cần danh sách phòng ban trước khi đăng nhập; POST/DELETE chỉ
 * ADMIN toàn cục.
 */
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> listAll() {
        return ResponseEntity.ok(departmentService.listAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
