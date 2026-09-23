package com.abe.system.abe_system.dto.attribute;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttributeRequest {

    @NotBlank
    @Size(max = 100)
    private String attributeName;

    @Size(max = 255)
    private String description;

    // Phòng ban phát hành attribute này. null = attribute toàn cục (chỉ ADMIN được
    // để trống trường này). DEPT_ADMIN bắt buộc bị ép về phòng ban của chính mình ở
    // tầng service, giá trị gửi lên đây (nếu có) chỉ có tác dụng với ADMIN.
    private Long departmentId;
}
