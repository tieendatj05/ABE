package com.abe.system.abe_system.dto.attribute;

import com.abe.system.abe_system.entity.UserAttribute;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserAttributeResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long attributeId;
    private String attributeName;
    private LocalDateTime assignedAt;
    private boolean revoked;
    private String issuedByUsername;

    public static UserAttributeResponse from(UserAttribute ua) {
        return new UserAttributeResponse(
                ua.getId(),
                ua.getUser().getId(),
                ua.getUser().getUsername(),
                ua.getAttribute().getId(),
                ua.getAttribute().getAttributeName(),
                ua.getAssignedAt(),
                ua.isRevoked(),
                ua.getIssuedBy() != null ? ua.getIssuedBy().getUsername() : null
        );
    }
}
