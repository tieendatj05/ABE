package com.abe.system.abe_system.twofactor;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sau khi username/password đúng nhưng tài khoản (ADMIN/DEPT_ADMIN) còn cần
 * xác thực bước 2 (TOTP), backend KHÔNG cấp JWT ngay - thay vào đó cấp 1
 * "vé tạm" (ticket) sống ngắn hạn, dùng 1 lần, để bước xác nhận mã 2FA biết
 * mật khẩu đã đúng rồi mà không cần gửi lại password lần nữa.
 *
 * Cố tình KHÔNG dùng JWT cho ticket này: JwtAuthenticationFilter chỉ kiểm tra
 * chữ ký + hạn dùng rồi cấp quyền đầy đủ ngay - nếu ticket là 1 JWT hợp lệ, nó
 * sẽ vô tình trở thành access token thật, phá vỡ hoàn toàn mục đích của 2FA.
 * Ticket ở đây là chuỗi ngẫu nhiên vô nghĩa với bên ngoài, chỉ tra được qua
 * map nội bộ này, không tự xác thực được như JWT.
 */
@Service
public class PendingTwoFactorService {

    private record PendingEntry(String username, Instant expiresAt) {
    }

    private static final Duration TICKET_TTL = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, PendingEntry> tickets = new ConcurrentHashMap<>();

    public String issueTicket(String username) {
        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, new PendingEntry(username, Instant.now().plus(TICKET_TTL)));
        return ticket;
    }

    /**
     * Tra ticket (KHÔNG xoá) - dùng để cho phép người dùng thử lại nếu gõ sai
     * mã 6 số, thay vì bắt đăng nhập lại từ đầu chỉ vì 1 lần gõ nhầm. Ticket
     * vẫn tự hết hạn sau {@link #TICKET_TTL} dù đúng hay sai.
     */
    public Optional<String> resolve(String ticket) {
        if (ticket == null) {
            return Optional.empty();
        }
        PendingEntry entry = tickets.get(ticket);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            tickets.remove(ticket);
            return Optional.empty();
        }
        return Optional.of(entry.username());
    }

    /**
     * Xoá ticket sau khi đã xác thực mã THÀNH CÔNG (chống dùng lại/replay 1
     * ticket đã hoàn tất để lấy JWT lần nữa).
     */
    public void consume(String ticket) {
        tickets.remove(ticket);
    }
}
