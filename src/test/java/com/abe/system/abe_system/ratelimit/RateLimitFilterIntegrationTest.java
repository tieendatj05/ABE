package com.abe.system.abe_system.ratelimit;

import com.abe.system.abe_system.entity.Role;
import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.repository.UserRepository;
import com.abe.system.abe_system.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test rate limit THẬT (rate-limit.enabled=true) - dùng @TestPropertySource
 * riêng biệt với application.properties mặc định của test (rate-limit.enabled
 * =false) để Spring dựng 1 ApplicationContext RIÊNG, có bean RateLimitService
 * độc lập, không chia sẻ bucket với các test khác trong bộ test chạy MockMvc
 * qua /api/auth/login hay /api/files/**.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "rate-limit.enabled=true",
        "rate-limit.login.max-per-minute=3",
        "rate-limit.download.max-per-minute=3"
})
@Transactional
class RateLimitFilterIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void loginIsRateLimitedPerIp() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", "no_such_user", "password", "whatever"));

        // 3 request dau trong cung 1 phut: KHONG bi chan boi rate limit (van co the
        // 401 vi sai user, nhung phai khac 429).
        for (int i = 0; i < 3; i++) {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content(body))
                    .andReturn();
            assertThat(result.getResponse().getStatus()).isNotEqualTo(429);
        }

        // Request thu 4 trong cung phut -> bi chan.
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().is(429));
    }

    @Test
    void downloadIsRateLimitedPerUser() throws Exception {
        userRepository.save(User.builder()
                .username("rl_download_user")
                .password(passwordEncoder.encode("Password123!"))
                .email("rl_download_user@test.com")
                .fullName("RL Download")
                .role(Role.DATA_USER)
                .build());

        // Sinh token truc tiep qua JwtService (giong het AuthService.buildAuthResponse)
        // thay vi goi that /api/auth/login - de KHONG tieu tan ngan sach rate-limit
        // cua rule login (dang test o phuong thuc khac trong cung context).
        String token = jwtService.generateToken(
                org.springframework.security.core.userdetails.User.builder()
                        .username("rl_download_user")
                        .password("x")
                        .authorities(List.of())
                        .build());

        // File id khong ton tai -> 404, nhung KHONG phai 429 cho toi khi vuot qua.
        for (int i = 0; i < 3; i++) {
            MvcResult result = mockMvc.perform(get("/api/files/999999/download")
                            .header("Authorization", "Bearer " + token))
                    .andReturn();
            assertThat(result.getResponse().getStatus()).isNotEqualTo(429);
        }

        mockMvc.perform(get("/api/files/999999/download")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(429));
    }
}
