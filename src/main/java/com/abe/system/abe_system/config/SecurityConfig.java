package com.abe.system.abe_system.config;

import com.abe.system.abe_system.ratelimit.RateLimitFilter;
import com.abe.system.abe_system.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cấu hình bảo mật tổng thể: xác thực bằng JWT (không dùng session/cookie),
 * BCrypt cho password, và phân quyền theo Role ngay ở tầng URL cho các API
 * quản lý thuộc tính (chỉ ADMIN mới được gọi).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API stateless dùng JWT, không có cookie session nên không cần CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Đổi mật khẩu của chính mình - đặt TRƯỚC rule permitAll chung của
                        // /api/auth/** bên dưới, vì path này CẦN đăng nhập (biết đang là ai).
                        .requestMatchers(HttpMethod.PATCH, "/api/auth/change-password").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Department là dữ liệu tham chiếu không nhạy cảm (tên khoa/phòng ban)
                        // và RegisterPage cần load danh sách này TRƯỚC KHI đăng nhập.
                        .requestMatchers(HttpMethod.GET, "/api/departments/**").permitAll()
                        .requestMatchers("/api/departments/**").hasRole("ADMIN")
                        // Đặt TRƯỚC rule ADMIN/DEPT_ADMIN catch-all bên dưới: user (mọi role) tự xem
                        // attribute của chính mình để frontend vẽ cây chính sách AND/OR.
                        .requestMatchers(HttpMethod.GET, "/api/attributes/me").authenticated()
                        // ABE phi tập trung hóa: DEPT_ADMIN cũng được quản lý attribute, nhưng
                        // chỉ trong phạm vi phòng ban của mình (kiểm tra ở AttributeService).
                        .requestMatchers("/api/attributes/**").hasAnyRole("ADMIN", "DEPT_ADMIN")
                        // Phong DEPT_ADMIN, và tạo thay tài khoản giảng viên/sinh viên, chỉ ADMIN
                        // toàn cục được làm - đặt TRƯỚC rule chung /api/users/**.
                        .requestMatchers(HttpMethod.POST, "/api/users/promote-dept-admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "DEPT_ADMIN")
                        // Data Owner xem lịch sử truy cập các file của chính mình - mọi role đã đăng nhập.
                        .requestMatchers(HttpMethod.GET, "/api/audit-logs/mine").authenticated()
                        .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")
                        // Upload file mã hoá - chỉ Data Owner được tạo file mới.
                        .requestMatchers(HttpMethod.POST, "/api/files/upload").hasRole("DATA_OWNER")
                        .requestMatchers("/api/files/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Đặt SAU JwtAuthenticationFilter: rate-limit theo username (endpoint download)
                // cần SecurityContext đã có Authentication trước đó.
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Cho phép frontend React (Vite dev server, http://localhost:5173) gọi API
     * từ origin khác. Lúc dev, Vite proxy /api sang backend nên không thực sự
     * cần CORS, nhưng vẫn khai báo để không vướng khi build/host frontend ở
     * origin khác backend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
