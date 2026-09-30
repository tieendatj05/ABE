package com.abe.system.abe_system.ratelimit;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý 1 "bucket" Bucket4j (thuật toán token bucket) cho mỗi key (vd
 * "login:<ip>" hoặc "download:<username>") - lưu thuần trong bộ nhớ
 * (ConcurrentHashMap), đủ dùng cho 1 instance app của đồ án, cùng triết lý
 * với LoginAttemptService (không cần Redis cho quy mô này). Mỗi key có cấu
 * hình sức chứa/tốc độ làm đầy lại (refill) RIÊNG do caller truyền vào lúc
 * gọi - 1 service dùng chung được cho nhiều rule khác nhau (login theo IP,
 * download theo user...).
 */
@Service
public class RateLimitService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @param key        định danh duy nhất cho rule + đối tượng bị giới hạn, vd "login:127.0.0.1"
     * @param maxRequest số request tối đa được phép trong 1 chu kỳ "period"
     * @param period     độ dài chu kỳ làm đầy lại (vd 1 phút)
     * @return true nếu request này được phép đi tiếp, false nếu đã vượt giới hạn
     */
    public boolean tryConsume(String key, int maxRequest, Duration period) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(maxRequest, period));
        return bucket.tryConsume(1);
    }

    private Bucket newBucket(int maxRequest, Duration period) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(maxRequest).refillGreedy(maxRequest, period))
                .build();
    }
}
