package com.abe.system.abe_system.dto.auth;

import com.abe.system.abe_system.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;
    private String username;
    private Role role;
    private Long departmentId;
    private String departmentName;
}
