package io.proxycheck.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void acquireWithinLimit() throws InterruptedException {
        var limiter = new RateLimiter(10);
        for (int i = 0; i < 10; i++) {
            limiter.acquire();
        }
    }

    @Test
    void tryAcquireReturnsTrue() {
        var limiter = new RateLimiter(5);
        assertTrue(limiter.tryAcquire());
    }

    @Test
    void tryAcquireReturnsFalseWhenExhausted() {
        var limiter = new RateLimiter(2);
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
    }

    @Test
    void refillsAfterOneSecond() throws InterruptedException {
        var limiter = new RateLimiter(2);
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());

        Thread.sleep(1100);

        assertTrue(limiter.tryAcquire());
    }

    @Test
    void rejectsInvalidRate() {
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(0));
        assertThrows(IllegalArgumentException.class, () -> new RateLimiter(-1));
    }
}
