package com.abe.system.abe_system.crypto;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShamirSecretSharingTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    void splitThenCombineWithAllSharesReturnsOriginalSecret() {
        BigInteger secret = randomSecret();

        List<ShamirSecretSharing.Share> shares = ShamirSecretSharing.split(secret, 5);
        BigInteger recovered = ShamirSecretSharing.combine(shares);

        assertThat(recovered).isEqualTo(secret);
    }

    @Test
    void combineFailsToRecoverSecretWhenAShareIsMissing() {
        BigInteger secret = randomSecret();

        List<ShamirSecretSharing.Share> shares = ShamirSecretSharing.split(secret, 4);
        List<ShamirSecretSharing.Share> partial = new ArrayList<>(shares.subList(0, 3));

        BigInteger recovered = ShamirSecretSharing.combine(partial);

        // Thiếu 1 share (threshold=4 nhưng chỉ đưa 3): kết quả sai lệch, KHÔNG được
        // trùng với secret gốc - đây chính là tính chất "cần đủ n" của threshold=n.
        assertThat(recovered).isNotEqualTo(secret);
    }

    @Test
    void singleChildAndNodeIsHandledCorrectly() {
        BigInteger secret = randomSecret();

        List<ShamirSecretSharing.Share> shares = ShamirSecretSharing.split(secret, 1);
        BigInteger recovered = ShamirSecretSharing.combine(shares);

        assertThat(shares).hasSize(1);
        assertThat(recovered).isEqualTo(secret);
    }

    private BigInteger randomSecret() {
        BigInteger secret;
        do {
            secret = new BigInteger(256, RANDOM);
        } while (secret.compareTo(ShamirSecretSharing.P) >= 0);
        return secret;
    }
}
