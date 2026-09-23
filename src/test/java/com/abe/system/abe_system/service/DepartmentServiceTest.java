package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.department.DepartmentRequest;
import com.abe.system.abe_system.dto.department.DepartmentResponse;
import com.abe.system.abe_system.entity.Attribute;
import com.abe.system.abe_system.entity.Department;
import com.abe.system.abe_system.repository.AttributeRepository;
import com.abe.system.abe_system.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DepartmentServiceTest {

    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private AttributeRepository attributeRepository;

    @Test
    void createListAndDeleteHappyPath() {
        DepartmentRequest request = new DepartmentRequest();
        request.setName("Khoa Nhi Test");
        request.setCode("NHI_TEST");

        DepartmentResponse created = departmentService.create(request);
        assertThat(created.getId()).isNotNull();
        assertThat(departmentService.listAll()).extracting(DepartmentResponse::getCode).contains("NHI_TEST");

        departmentService.delete(created.getId());
        assertThat(departmentRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void deleteBlockedWhenDepartmentStillReferenced() {
        Department department = departmentRepository.save(
                Department.builder().name("Khoa Xet Nghiem Test").code("XN_TEST").build());
        attributeRepository.save(Attribute.builder()
                .attributeName("position:kts_test").issuerDepartment(department).build());

        assertThatThrownBy(() -> departmentService.delete(department.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
