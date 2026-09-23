package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.attribute.AttributeRequest;
import com.abe.system.abe_system.dto.attribute.AttributeResponse;
import com.abe.system.abe_system.dto.attribute.UserAttributeResponse;
import com.abe.system.abe_system.entity.Attribute;
import com.abe.system.abe_system.entity.Department;
import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.repository.AttributeRepository;
import com.abe.system.abe_system.repository.DepartmentRepository;
import com.abe.system.abe_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra phạm vi phân quyền của AttributeService trong mô hình ABE phi
 * tập trung hóa: ADMIN toàn quyền, DEPT_ADMIN chỉ thao tác được attribute/user
 * thuộc phòng ban của chính mình.
 */
@SpringBootTest
@Transactional
class AttributeServiceTest {

    @Autowired
    private AttributeService attributeService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private AttributeRepository attributeRepository;

    private Department khoaNoi;
    private Department khoaNgoai;
    private User admin;
    private User deptAdminNoi;
    private User staffNoi;
    private User staffNgoai;

    @BeforeEach
    void setUp() {
        khoaNoi = departmentRepository.save(Department.builder().name("Khoa Noi").code("NOI").build());
        khoaNgoai = departmentRepository.save(Department.builder().name("Khoa Ngoai").code("NGOAI").build());

        admin = userRepository.save(User.builder()
                .username("attr_admin").password("x").email("attr_admin@test.com")
                .fullName("Admin").role(Role.ADMIN).build());
        deptAdminNoi = userRepository.save(User.builder()
                .username("attr_dept_admin_noi").password("x").email("attr_dept_admin_noi@test.com")
                .fullName("Dept Admin Noi").role(Role.DEPT_ADMIN).department(khoaNoi).build());
        staffNoi = userRepository.save(User.builder()
                .username("attr_staff_noi").password("x").email("attr_staff_noi@test.com")
                .fullName("Staff Noi").role(Role.DATA_USER).department(khoaNoi).build());
        staffNgoai = userRepository.save(User.builder()
                .username("attr_staff_ngoai").password("x").email("attr_staff_ngoai@test.com")
                .fullName("Staff Ngoai").role(Role.DATA_USER).department(khoaNgoai).build());
    }

    @Test
    void adminCanCreateGlobalAndDepartmentScopedAttributes() {
        AttributeRequest global = new AttributeRequest();
        global.setAttributeName("role:ADMIN_TEST");
        AttributeResponse globalResponse = attributeService.create(global, admin.getUsername());
        assertThat(globalResponse.getIssuerDepartmentId()).isNull();

        AttributeRequest scoped = new AttributeRequest();
        scoped.setAttributeName("position:bac_si_noi_test");
        scoped.setDepartmentId(khoaNoi.getId());
        AttributeResponse scopedResponse = attributeService.create(scoped, admin.getUsername());
        assertThat(scopedResponse.getIssuerDepartmentId()).isEqualTo(khoaNoi.getId());
    }

    @Test
    void deptAdminCanOnlyCreateAttributesForOwnDepartment() {
        AttributeRequest own = new AttributeRequest();
        own.setAttributeName("position:dieu_duong_noi_test");
        AttributeResponse created = attributeService.create(own, deptAdminNoi.getUsername());
        assertThat(created.getIssuerDepartmentId()).isEqualTo(khoaNoi.getId());

        AttributeRequest otherDept = new AttributeRequest();
        otherDept.setAttributeName("position:should_fail_test");
        otherDept.setDepartmentId(khoaNgoai.getId());
        assertThatThrownBy(() -> attributeService.create(otherDept, deptAdminNoi.getUsername()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deptAdminCannotDeleteGlobalOrOtherDepartmentsAttribute() {
        Attribute global = attributeRepository.save(
                Attribute.builder().attributeName("role:GLOBAL_TEST").build());
        Attribute otherDept = attributeRepository.save(
                Attribute.builder().attributeName("position:ngoai_test").issuerDepartment(khoaNgoai).build());

        assertThatThrownBy(() -> attributeService.delete(global.getId(), deptAdminNoi.getUsername()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> attributeService.delete(otherDept.getId(), deptAdminNoi.getUsername()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deptAdminCanOnlyAssignOwnDepartmentAttributeToOwnDepartmentUser() {
        Attribute ownAttribute = attributeRepository.save(
                Attribute.builder().attributeName("position:bac_si_noi_test2").issuerDepartment(khoaNoi).build());
        Attribute otherAttribute = attributeRepository.save(
                Attribute.builder().attributeName("position:ngoai_test2").issuerDepartment(khoaNgoai).build());

        // Dung attribute, dung user cung phong ban -> thanh cong.
        UserAttributeResponse response = attributeService.assign(
                staffNoi.getId(), ownAttribute.getId(), deptAdminNoi.getUsername());
        assertThat(response.getIssuedByUsername()).isEqualTo(deptAdminNoi.getUsername());

        // Dung attribute nhung sai user (khac phong ban) -> tu choi.
        assertThatThrownBy(() -> attributeService.assign(
                staffNgoai.getId(), ownAttribute.getId(), deptAdminNoi.getUsername()))
                .isInstanceOf(AccessDeniedException.class);

        // Sai attribute (khac phong ban) -> tu choi du user dung phong ban.
        assertThatThrownBy(() -> attributeService.assign(
                staffNoi.getId(), otherAttribute.getId(), deptAdminNoi.getUsername()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deptAdminListByUserRestrictedToOwnDepartment() {
        assertThat(attributeService.listByUser(staffNoi.getId(), deptAdminNoi.getUsername())).isNotNull();
        assertThatThrownBy(() -> attributeService.listByUser(staffNgoai.getId(), deptAdminNoi.getUsername()))
                .isInstanceOf(AccessDeniedException.class);
    }
}
