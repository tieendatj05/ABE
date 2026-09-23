package com.abe.system.abe_system.dto.department;

import com.abe.system.abe_system.entity.Department;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DepartmentResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private LocalDateTime createdAt;

    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCode(),
                department.getDescription(),
                department.getCreatedAt()
        );
    }
}
