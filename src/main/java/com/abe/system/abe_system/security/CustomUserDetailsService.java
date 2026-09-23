package com.abe.system.abe_system.security;

import com.abe.system.abe_system.entity.User;
import com.abe.system.abe_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Cầu nối giữa entity User (do chúng ta tự định nghĩa) và UserDetails
 * (interface mà Spring Security hiểu). Role được map thành authority dạng
 * "ROLE_ADMIN"/"ROLE_DATA_OWNER"/"ROLE_DATA_USER" để dùng được với
 * hasRole("ADMIN") ở SecurityConfig/@PreAuthorize.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}
