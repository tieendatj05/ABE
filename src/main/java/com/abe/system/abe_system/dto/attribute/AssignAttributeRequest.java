package com.abe.system.abe_system.dto.attribute;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignAttributeRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long attributeId;
}
