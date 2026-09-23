package com.abe.system.abe_system.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Mã hoá/giải mã nội dung file bằng AES-256/GCM, và chuyển đổi khoá AES <->
 * BigInteger để dùng làm "secret" cho {@link ShamirSecretSharing}.
 *
 * IV (12 byte) sinh ngẫu nhiên mỗi lần mã hoá, ghi liền vào ĐẦU ciphertext lưu
 * trên đĩa (không cần cột riêng trong DB) - GCM cần đúng IV để giải mã nhưng
 * IV không cần giữ bí mật.
 */
public final class AesFileCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE_BYTES = 32; // AES-256
    private static final int IV_SIZE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private AesFileCipher() {
    }

    public static byte[] generateKey() {
        byte[] key = new byte[KEY_SIZE_BYTES];
        RANDOM.nextBytes(key);
        return key;
    }

    public static byte[] encrypt(byte[] plaintext, byte[] key) {
        try {
            byte[] iv = new byte[IV_SIZE_BYTES];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] out = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ciphertext, 0, out, iv.length, ciphertext.length);
            return out;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Lỗi mã hoá file", e);
        }
    }

    public static byte[] decrypt(byte[] ivAndCiphertext, byte[] key) {
        try {
            byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, IV_SIZE_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, IV_SIZE_BYTES, ivAndCiphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Lỗi giải mã file (khoá sai hoặc dữ liệu bị hỏng)", e);
        }
    }

    /** Khoá AES (32 byte) -> BigInteger dương < 2^256, luôn nhỏ hơn {@link ShamirSecretSharing#P}. */
    public static BigInteger keyToSecret(byte[] key) {
        return new BigInteger(1, key);
    }

    /** Chiều ngược lại của {@link #keyToSecret}, luôn trả về đúng 32 byte (đệm 0 bên trái nếu cần). */
    public static byte[] secretToKey(BigInteger secret) {
        byte[] raw = secret.toByteArray();
        byte[] key = new byte[KEY_SIZE_BYTES];
        int copyLen = Math.min(raw.length, KEY_SIZE_BYTES);
        System.arraycopy(raw, raw.length - copyLen, key, KEY_SIZE_BYTES - copyLen, copyLen);
        return key;
    }
}
