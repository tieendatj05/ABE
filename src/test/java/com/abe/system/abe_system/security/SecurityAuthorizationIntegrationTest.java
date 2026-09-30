package com.abe.system.abe_system.security;

import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tích hợp toàn bộ chuỗi bảo mật web: login (AuthController) sinh JWT,
 * JwtAuthenticationFilter đọc token, và các rule phân quyền theo role khai
 * báo trong SecurityConfig - trước khi có test này, tầng web/security chỉ
 * được "hiểu ngầm" là đúng qua code, không có gì xác nhận tự động.
 * Chạy trên H2 in-memory (src/test/resources/application.properties).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private User createUser(String username, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode("password123"))
                .email(username + "@test.com")
                .fullName(username)
                .role(role)
                .build());
    }

    private String loginAndGetToken(String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", "password123"));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);

        // ADMIN/DEPT_ADMIN bắt buộc 2FA (xem AuthService.login) - user vừa tạo trong
        // test luôn ở lần đăng nhập ĐẦU nên hệ thống trả về SETUP_REQUIRED kèm secret;
        // tự hoàn tất bước quét QR bằng cách sinh đúng mã 6 số hiện tại từ secret đó,
        // để các test khác (vd tạo phòng ban) không phải tự lo phần 2FA.
        if (json.hasNonNull("twoFactorChallenge")) {
            String ticket = json.get("twoFactorTicket").asText();
            String secret = json.get("manualEntryKey").asText();
            String code = currentTotpCode(secret);
            String confirmBody = objectMapper.writeValueAsString(Map.of("ticket", ticket, "code", code));
            String confirmResponse = mockMvc.perform(post("/api/auth/2fa/confirm-setup")
                            .contentType("application/json")
                            .content(confirmBody))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return objectMapper.readTree(confirmResponse).get("token").asText();
        }

        return json.get("token").asText();
    }

    private String currentTotpCode(String secret) throws Exception {
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        TimeProvider timeProvider = new SystemTimeProvider();
        return codeGenerator.generate(secret, timeProvider.getTime() / 30);
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        createUser("owner_wrong_pw", Role.DATA_OWNER);
        String body = objectMapper.writeValueAsString(Map.of("username", "owner_wrong_pw", "password", "nope"));

        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/files"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void protectedEndpointWithInvalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/files").header("Authorization", "Bearer garbage.not.a.jwt"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void nonAdminCannotCreateDepartment() throws Exception {
        createUser("owner_dept", Role.DATA_OWNER);
        String token = loginAndGetToken("owner_dept");
        String body = objectMapper.writeValueAsString(Map.of("name", "Khoa Ngoai", "code", "NGOAI"));

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateDepartment() throws Exception {
        createUser("admin_dept", Role.ADMIN);
        String token = loginAndGetToken("admin_dept");
        String body = objectMapper.writeValueAsString(Map.of("name", "Khoa Ngoai 2", "code", "NGOAI2"));

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void departmentListIsPublicWithoutToken() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk());
    }

    @Test
    void onlyDataOwnerCanUploadFile() throws Exception {
        createUser("plain_user", Role.DATA_USER);
        String token = loginAndGetToken("plain_user");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/files/upload")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "a.txt", "text/plain", "abc".getBytes()))
                        .param("accessPolicy", "role:ADMIN")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
