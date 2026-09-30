package com.abe.system.abe_system.service;

import com.abe.system.abe_system.exception.ContentIntegrityException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentIntegrityServiceTest {

    private final ContentIntegrityService service = new ContentIntegrityService();

    @Test
    void sha256HexIsDeterministicAndCorrectLength() {
        byte[] content = "noi dung bat ky".getBytes(StandardCharsets.UTF_8);

        String hash1 = service.sha256Hex(content);
        String hash2 = service.sha256Hex(content);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 = 32 byte = 64 ky tu hex
        assertThat(hash1).matches("^[0-9a-f]{64}$");
    }

    @Test
    void differentContentProducesDifferentHash() {
        String hashA = service.sha256Hex("noi dung A".getBytes(StandardCharsets.UTF_8));
        String hashB = service.sha256Hex("noi dung B".getBytes(StandardCharsets.UTF_8));

        assertThat(hashA).isNotEqualTo(hashB);
    }

    @Test
    void verifyPassesWhenHashMatches() {
        byte[] content = "noi dung dung".getBytes(StandardCharsets.UTF_8);
        String hash = service.sha256Hex(content);

        service.verify(hash, content); // khong nem loi la dat
    }

    @Test
    void verifyThrowsWhenHashDoesNotMatch() {
        byte[] content = "noi dung that".getBytes(StandardCharsets.UTF_8);
        String wrongHash = service.sha256Hex("noi dung khac".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.verify(wrongHash, content))
                .isInstanceOf(ContentIntegrityException.class)
                .hasMessage("Tài liệu đã bị can thiệp");
    }

    @Test
    void verifySkipsCheckWhenExpectedHashIsNull() {
        byte[] content = "bat ky noi dung nao".getBytes(StandardCharsets.UTF_8);

        // File cu chua co hash (null) - khong duoc bao loi.
        service.verify(null, content);
    }
}
