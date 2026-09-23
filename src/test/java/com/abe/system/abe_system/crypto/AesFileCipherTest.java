package com.abe.system.abe_system.crypto;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesFileCipherTest {

    @Test
    void encryptThenDecryptReturnsOriginalPlaintext() {
        byte[] key = AesFileCipher.generateKey();
        byte[] plaintext = "noi dung file bi mat".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = AesFileCipher.encrypt(plaintext, key);
        byte[] decrypted = AesFileCipher.decrypt(ciphertext, key);

        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(ciphertext).isNotEqualTo(plaintext);
    }

    @Test
    void decryptWithWrongKeyFails() {
        byte[] key = AesFileCipher.generateKey();
        byte[] wrongKey = AesFileCipher.generateKey();
        byte[] ciphertext = AesFileCipher.encrypt("secret".getBytes(StandardCharsets.UTF_8), key);

        assertThatThrownBy(() -> AesFileCipher.decrypt(ciphertext, wrongKey))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void keyToSecretAndBackRoundTripsExactly() {
        byte[] key = AesFileCipher.generateKey();

        BigInteger secret = AesFileCipher.keyToSecret(key);
        byte[] roundTripped = AesFileCipher.secretToKey(secret);

        assertThat(roundTripped).isEqualTo(key);
        assertThat(secret.compareTo(ShamirSecretSharing.P)).isLessThan(0);
    }
}
