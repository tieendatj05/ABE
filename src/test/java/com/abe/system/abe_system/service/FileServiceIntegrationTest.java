package com.abe.system.abe_system.service;

import com.abe.system.abe_system.dto.file.FileResponse;
import com.abe.system.abe_system.entity.AuditAction;
import com.abe.system.abe_system.entity.AuditLog;
import com.abe.system.abe_system.entity.Attribute;
import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.entity.UserAttribute;
import com.abe.system.abe_system.repository.AttributeRepository;
import com.abe.system.abe_system.repository.AuditLogRepository;
import com.abe.system.abe_system.repository.UserAttributeRepository;
import com.abe.system.abe_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test tích hợp luồng chính của lõi ABE: upload (mã hoá + chia khoá theo
 * policy) rồi download (chỉ user có đủ attribute còn hiệu lực mới giải mã
 * được) - chạy trên H2 in-memory (xem src/test/resources/application.properties),
 * không cần Postgres thật.
 */
@SpringBootTest
@Transactional
class FileServiceIntegrationTest {

    @Autowired
    private FileService fileService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AttributeRepository attributeRepository;
    @Autowired
    private UserAttributeRepository userAttributeRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void ownerUploadsFileAndUserWithMatchingAttributeCanDecryptIt() {
        Attribute deptAttribute = attributeRepository.save(
                Attribute.builder().attributeName("department:CNTT").description("Khoa CNTT").build());

        User owner = userRepository.save(User.builder()
                .username("owner1").password("x").email("owner1@test.com")
                .fullName("Owner One").role(Role.DATA_OWNER).build());
        User allowedUser = userRepository.save(User.builder()
                .username("user1").password("x").email("user1@test.com")
                .fullName("User One").role(Role.DATA_USER).build());
        User strangerUser = userRepository.save(User.builder()
                .username("user2").password("x").email("user2@test.com")
                .fullName("User Two").role(Role.DATA_USER).build());

        userAttributeRepository.save(UserAttribute.builder().user(allowedUser).attribute(deptAttribute).build());

        byte[] originalContent = "noi dung tai lieu mat".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "bao_cao.txt", "text/plain", originalContent);

        FileResponse uploaded = fileService.upload(file, "department:CNTT", owner);

        FileService.DownloadResult downloaded = fileService.download(uploaded.getId(), allowedUser);
        assertThat(downloaded.content()).isEqualTo(originalContent);
        assertThat(downloaded.fileName()).isEqualTo("bao_cao.txt");

        assertThatThrownBy(() -> fileService.download(uploaded.getId(), strangerUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void revokedAttributeNoLongerAllowsDecryption() {
        Attribute attribute = attributeRepository.save(
                Attribute.builder().attributeName("role:ADMIN").description("Admin").build());

        User owner = userRepository.save(User.builder()
                .username("owner2").password("x").email("owner2@test.com")
                .fullName("Owner Two").role(Role.DATA_OWNER).build());
        User user = userRepository.save(User.builder()
                .username("user3").password("x").email("user3@test.com")
                .fullName("User Three").role(Role.DATA_USER).build());

        UserAttribute userAttribute = userAttributeRepository.save(
                UserAttribute.builder().user(user).attribute(attribute).build());

        byte[] content = "noi dung".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", content);
        FileResponse uploaded = fileService.upload(file, "role:ADMIN", owner);

        // Truoc khi revoke: tai duoc binh thuong.
        assertThat(fileService.download(uploaded.getId(), user).content()).isEqualTo(content);

        userAttribute.setRevoked(true);
        userAttributeRepository.save(userAttribute);

        assertThatThrownBy(() -> fileService.download(uploaded.getId(), user))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ownerOrAdminCanDeleteFileButOthersCannot() {
        Attribute attribute = attributeRepository.save(
                Attribute.builder().attributeName("role:ADMIN").description("Admin").build());

        User owner = userRepository.save(User.builder()
                .username("owner3").password("x").email("owner3@test.com")
                .fullName("Owner Three").role(Role.DATA_OWNER).build());
        User stranger = userRepository.save(User.builder()
                .username("user4").password("x").email("user4@test.com")
                .fullName("User Four").role(Role.DATA_USER).build());

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.txt", "text/plain", "abc".getBytes(StandardCharsets.UTF_8));
        FileResponse uploaded = fileService.upload(file, attribute.getAttributeName(), owner);

        assertThatThrownBy(() -> fileService.delete(uploaded.getId(), stranger))
                .isInstanceOf(AccessDeniedException.class);

        fileService.delete(uploaded.getId(), owner);

        assertThatThrownBy(() -> fileService.download(uploaded.getId(), owner))
                .isInstanceOf(com.abe.system.abe_system.exception.ResourceNotFoundException.class);
    }

    @Test
    void uploadAndDownloadWriteAuditLogEntries() {
        Attribute deptAttribute = attributeRepository.save(
                Attribute.builder().attributeName("department:NOI").description("Khoa Noi").build());

        User owner = userRepository.save(User.builder()
                .username("audit_owner1").password("x").email("audit_owner1@test.com")
                .fullName("Audit Owner").role(Role.DATA_OWNER).build());
        User allowedUser = userRepository.save(User.builder()
                .username("audit_user1").password("x").email("audit_user1@test.com")
                .fullName("Audit User").role(Role.DATA_USER).build());

        userAttributeRepository.save(UserAttribute.builder().user(allowedUser).attribute(deptAttribute).build());

        MockMultipartFile file = new MockMultipartFile(
                "file", "ho_so.txt", "text/plain", "abc".getBytes(StandardCharsets.UTF_8));
        FileResponse uploaded = fileService.upload(file, "department:NOI", owner);
        fileService.download(uploaded.getId(), allowedUser);

        List<AuditLog> logs = auditLogRepository.findByFileIdOrderByOccurredAtDesc(uploaded.getId());
        assertThat(logs).extracting(AuditLog::getAction)
                .containsExactlyInAnyOrder(AuditAction.UPLOAD, AuditAction.DOWNLOAD_SUCCESS);
        assertThat(logs).allMatch(l -> l.getFileName().equals("ho_so.txt"));
    }

    @Test
    void deniedDownloadWritesAuditLogWithAttributeDetail() {
        Attribute attribute = attributeRepository.save(
                Attribute.builder().attributeName("department:NGOAI").description("Khoa Ngoai").build());

        User owner = userRepository.save(User.builder()
                .username("audit_owner2").password("x").email("audit_owner2@test.com")
                .fullName("Audit Owner Two").role(Role.DATA_OWNER).build());
        User stranger = userRepository.save(User.builder()
                .username("audit_user2").password("x").email("audit_user2@test.com")
                .fullName("Audit Stranger").role(Role.DATA_USER).build());

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.txt", "text/plain", "abc".getBytes(StandardCharsets.UTF_8));
        FileResponse uploaded = fileService.upload(file, "department:NGOAI", owner);

        assertThatThrownBy(() -> fileService.download(uploaded.getId(), stranger))
                .isInstanceOf(AccessDeniedException.class);

        List<AuditLog> logs = auditLogRepository.findByFileIdOrderByOccurredAtDesc(uploaded.getId());
        AuditLog denied = logs.stream().filter(l -> l.getAction() == AuditAction.DOWNLOAD_DENIED).findFirst()
                .orElseThrow();
        assertThat(denied.getDetail()).isNotBlank();
        assertThat(denied.getAccessPolicy()).isEqualTo("department:NGOAI");
    }

    @Test
    void deleteWritesAuditLogEntryThatSurvivesFileDeletion() {
        Attribute attribute = attributeRepository.save(
                Attribute.builder().attributeName("role:ADMIN").description("Admin").build());

        User owner = userRepository.save(User.builder()
                .username("audit_owner3").password("x").email("audit_owner3@test.com")
                .fullName("Audit Owner Three").role(Role.DATA_OWNER).build());

        MockMultipartFile file = new MockMultipartFile(
                "file", "delete_me.txt", "text/plain", "abc".getBytes(StandardCharsets.UTF_8));
        FileResponse uploaded = fileService.upload(file, "role:ADMIN", owner);
        Long fileId = uploaded.getId();

        fileService.delete(fileId, owner);

        List<AuditLog> logs = auditLogRepository.findByFileIdOrderByOccurredAtDesc(fileId);
        assertThat(logs).extracting(AuditLog::getAction).contains(AuditAction.DELETE);
        assertThat(logs).allMatch(l -> l.getFileName().equals("delete_me.txt"));
    }
}
