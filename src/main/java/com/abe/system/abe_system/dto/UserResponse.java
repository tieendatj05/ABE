package com.abe.system.abe_system.dto;

import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * View trả về cho client - KHÔNG bao giờ chứa trường password.
 */
@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private LocalDateTime createdAt;
    private Long departmentId;
    private String departmentName;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getDepartment() != null ? user.getDepartment().getName() : null
        );
    }
}
