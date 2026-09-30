package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.UserResponse;
import com.abe.system.abe_system.dto.auth.RegisterRequest;
import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.exception.DuplicateResourceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ADMIN tạo thay tài khoản giảng viên/sinh viên (tài khoản do nhà trường cấp,
 * không qua form tự đăng ký công khai - xem AuthServiceTest.selfRegisteringAsDataOwnerIsRejected).
 */
@SpringBootTest
@Transactional
class UserAdminServiceTest {

    @Autowired
    private UserAdminService userAdminService;

    private RegisterRequest baseRequest(String username, Role role) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword("secret123");
        request.setEmail(username + "@test.com");
        request.setFullName("Test " + username);
        request.setRole(role);
        return request;
    }

    @Test
    void adminCanCreateTeacherAccount() {
        UserResponse response = userAdminService.createUser(baseRequest("teacher_created_by_admin", Role.DATA_OWNER));
        assertThat(response.getRole()).isEqualTo(Role.DATA_OWNER);
        assertThat(response.getUsername()).isEqualTo("teacher_created_by_admin");
    }

    @Test
    void adminCanCreateStudentAccount() {
        UserResponse response = userAdminService.createUser(baseRequest("student_created_by_admin", Role.DATA_USER));
        assertThat(response.getRole()).isEqualTo(Role.DATA_USER);
    }

    @Test
    void cannotCreateAdminOrDeptAdminThroughThisEndpoint() {
        assertThatThrownBy(() -> userAdminService.createUser(baseRequest("sneaky_admin", Role.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userAdminService.createUser(baseRequest("sneaky_dept_admin", Role.DEPT_ADMIN)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateUsernameIsRejected() {
        userAdminService.createUser(baseRequest("dup_teacher", Role.DATA_OWNER));
        RegisterRequest duplicate = baseRequest("dup_teacher", Role.DATA_OWNER);
        duplicate.setEmail("different_email@test.com");

        assertThatThrownBy(() -> userAdminService.createUser(duplicate))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
