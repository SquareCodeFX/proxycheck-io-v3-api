package io.proxycheck.api;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Token bucket rate limiter using a {@link Semaphore}. Permits are refilled to the
 * configured maximum once per second. Thread-safe via volatile read + double-checked
 * locking on the refill path.
 *
 * <p>The blocking {@link #acquire()} method sleeps precisely until the next window
 * refill instead of polling at a fixed interval, reducing both CPU waste and latency.
 */
final class RateLimiter {

    private final int maxRequestsPerSecond;
    private final Semaphore semaphore;
    private volatile long windowStart;
    private final Object refillLock = new Object();

    RateLimiter(int maxRequestsPerSecond) {
        if (maxRequestsPerSecond < 1) {
            throw new IllegalArgumentException("maxRequestsPerSecond must be >= 1");
        }
        this.maxRequestsPerSecond = maxRequestsPerSecond;
        this.semaphore = new Semaphore(maxRequestsPerSecond);
        this.windowStart = System.nanoTime();
    }

    void acquire() throws InterruptedException {
        while (true) {
            refillIfNeeded();
            if (semaphore.tryAcquire()) {
                return;
            }
            // Sleep until the current window expires instead of polling at fixed intervals
            long remainingNanos = 1_000_000_000L - (System.nanoTime() - windowStart);
            if (remainingNanos > 0) {
                TimeUnit.NANOSECONDS.sleep(remainingNanos);
            }
            refillIfNeeded();
            if (semaphore.tryAcquire()) {
                return;
            }
        }
    }

    boolean tryAcquire() {
        refillIfNeeded();
        return semaphore.tryAcquire();
    }

    // Double-checked locking: fast volatile read avoids synchronization on the
    // common path; only one thread performs the actual refill per window.
    private void refillIfNeeded() {
        long now = System.nanoTime();
        long elapsed = now - windowStart;
        if (elapsed >= 1_000_000_000L) {
            synchronized (refillLock) {
                // Re-check inside lock since another thread may have already refilled
                elapsed = System.nanoTime() - windowStart;
                if (elapsed >= 1_000_000_000L) {
                    int currentPermits = semaphore.availablePermits();
                    int toRelease = maxRequestsPerSecond - currentPermits;
                    if (toRelease > 0) {
                        semaphore.release(toRelease);
                    }
                    windowStart = System.nanoTime();
                }
            }
        }
    }
}
