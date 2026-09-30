package com.abe.system.abe_system.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    @Test
    void allowsUpToMaxRequestsThenBlocks() {
        RateLimitService service = new RateLimitService();
        String key = "test-key";

        assertThat(service.tryConsume(key, 3, Duration.ofMinutes(1))).isTrue();
        assertThat(service.tryConsume(key, 3, Duration.ofMinutes(1))).isTrue();
        assertThat(service.tryConsume(key, 3, Duration.ofMinutes(1))).isTrue();
        // Lan thu 4 trong cung chu ky -> vuot gioi han.
        assertThat(service.tryConsume(key, 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void differentKeysHaveIndependentLimits() {
        RateLimitService service = new RateLimitService();

        assertThat(service.tryConsume("key-a", 1, Duration.ofMinutes(1))).isTrue();
        assertThat(service.tryConsume("key-a", 1, Duration.ofMinutes(1))).isFalse();
        // key khac (vd IP/username khac) khong bi anh huong boi key-a.
        assertThat(service.tryConsume("key-b", 1, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void refillsAfterPeriodElapses() throws InterruptedException {
        RateLimitService service = new RateLimitService();
        String key = "refill-key";
        Duration shortPeriod = Duration.ofMillis(150);

        assertThat(service.tryConsume(key, 1, shortPeriod)).isTrue();
        assertThat(service.tryConsume(key, 1, shortPeriod)).isFalse();

        Thread.sleep(250);

        assertThat(service.tryConsume(key, 1, shortPeriod)).isTrue();
    }
}
