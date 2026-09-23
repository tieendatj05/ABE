package com.abe.system.abe_system.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Shamir Secret Sharing thuần, cài trên GF(P) với P là 1 số nguyên tố ~261-bit
 * CỐ ĐỊNH (đủ lớn hơn khoá AES-256 xem như BigInteger dương < 2^256).
 *
 * P phải cố định vĩnh viễn qua các lần chạy app: share sinh ra lúc upload file
 * được lưu vào DB (FileMetadata.encryptedAesKey), phải combine lại đúng bằng
 * CHÍNH P này ở lần download sau (có thể sau khi restart app) - nên không được
 * sinh P ngẫu nhiên mỗi lần khởi động.
 */
public final class ShamirSecretSharing {

    /** Số nguyên tố ~261-bit, sinh 1 lần bằng openssl (openssl prime -generate -bits 261 -hex), cố định vĩnh viễn. */
    public static final BigInteger P =
            new BigInteger("1A98FF173D116A46F1A46AA6F734470E1DE90B3BC57B611BC3258DE99B561371CF", 16);

    private static final SecureRandom RANDOM = new SecureRandom();

    private ShamirSecretSharing() {
    }

    public record Share(int x, BigInteger y) {
    }

    /**
     * Chia "secret" thành đúng "n" share sao cho phải có ĐỦ CẢ n share mới
     * ghép lại được secret gốc (threshold = n, đa thức bậc n-1) - dùng cho
     * ngữ nghĩa AND (n nhánh con, cần đủ tất cả) trong cây chính sách.
     */
    public static List<Share> split(BigInteger secret, int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Số share phải >= 1");
        }
        if (secret.signum() < 0 || secret.compareTo(P) >= 0) {
            throw new IllegalArgumentException("Secret phải nằm trong khoảng [0, P)");
        }

        // f(x) = secret + a1*x + a2*x^2 + ... + a(n-1)*x^(n-1) mod P, hệ số ngẫu
        // nhiên -> f(0) = secret. Cần đủ n điểm (x=1..n) mới nội suy lại được f.
        BigInteger[] coefficients = new BigInteger[n];
        coefficients[0] = secret;
        for (int i = 1; i < n; i++) {
            coefficients[i] = randomFieldElement();
        }

        List<Share> shares = new ArrayList<>(n);
        for (int x = 1; x <= n; x++) {
            shares.add(new Share(x, evaluate(coefficients, x)));
        }
        return shares;
    }

    /**
     * Ghép lại secret gốc từ đúng "n" share (nội suy Lagrange tại x=0) - phải
     * có đủ toàn bộ share sinh ra lúc split (đúng threshold = n), thiếu 1 share
     * cũng không nội suy đúng được.
     */
    public static BigInteger combine(List<Share> shares) {
        BigInteger secret = BigInteger.ZERO;
        for (Share share : shares) {
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;
            for (Share other : shares) {
                if (other.x() == share.x()) {
                    continue;
                }
                numerator = numerator.multiply(BigInteger.valueOf(-other.x())).mod(P);
                denominator = denominator.multiply(BigInteger.valueOf(share.x() - other.x())).mod(P);
            }
            BigInteger lagrangeCoefficient = numerator.multiply(denominator.modInverse(P)).mod(P);
            secret = secret.add(share.y().multiply(lagrangeCoefficient)).mod(P);
        }
        return secret;
    }

    private static BigInteger evaluate(BigInteger[] coefficients, int x) {
        BigInteger result = BigInteger.ZERO;
        BigInteger xBig = BigInteger.valueOf(x);
        BigInteger xPower = BigInteger.ONE;
        for (BigInteger coefficient : coefficients) {
            result = result.add(coefficient.multiply(xPower)).mod(P);
            xPower = xPower.multiply(xBig).mod(P);
        }
        return result;
    }

    private static BigInteger randomFieldElement() {
        BigInteger value;
        do {
            value = new BigInteger(P.bitLength(), RANDOM);
        } while (value.compareTo(P) >= 0);
        return value;
    }
}
