package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.auth.AuthResponse;
import com.abe.system.abe_system.dto.auth.RegisterRequest;
import com.abe.system.abe_system.entity.Department;
import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void selfRegisteringAsDeptAdminIsRejected() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("would_be_dept_admin");
        request.setPassword("secret123");
        request.setEmail("would_be_dept_admin@test.com");
        request.setFullName("Nope");
        request.setRole(Role.DEPT_ADMIN);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registeringWithDepartmentIdAttachesDepartment() {
        Department department = departmentRepository.save(
                Department.builder().name("Khoa Test Auth").code("AUTH_TEST").build());

        RegisterRequest request = new RegisterRequest();
        request.setUsername("owner_with_dept");
        request.setPassword("secret123");
        request.setEmail("owner_with_dept@test.com");
        request.setFullName("Owner With Dept");
        request.setRole(Role.DATA_OWNER);
        request.setDepartmentId(department.getId());

        AuthResponse response = authService.register(request);
        assertThat(response.getDepartmentId()).isEqualTo(department.getId());
        assertThat(response.getDepartmentName()).isEqualTo("Khoa Test Auth");
    }
}
