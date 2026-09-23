package com.abe.system.abe_system.dto.attribute;

import com.abe.system.abe_system.entity.Attribute;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AttributeResponse {

    private Long id;
    private String attributeName;
    private String description;
    private LocalDateTime createdAt;
    private Long issuerDepartmentId;
    private String issuerDepartmentName;

    public static AttributeResponse from(Attribute attribute) {
        return new AttributeResponse(
                attribute.getId(),
                attribute.getAttributeName(),
                attribute.getDescription(),
                attribute.getCreatedAt(),
                attribute.getIssuerDepartment() != null ? attribute.getIssuerDepartment().getId() : null,
                attribute.getIssuerDepartment() != null ? attribute.getIssuerDepartment().getName() : null
        );
    }
}
