package io.proxycheck.api;

import java.time.Duration;
import java.util.Objects;

/**
 * Configures retry behavior with exponential backoff for transient API failures.
 *
 * <p>Default policy: 3 retries with delays of 500ms, 1s, 2s (capped at 8s).
 * Only {@link java.io.IOException} and HTTP 5xx errors are retried;
 * client errors (4xx) are never retried.
 */
public final class RetryPolicy {

    private final int maxRetries;
    private final Duration initialDelay;
    private final double multiplier;
    private final Duration maxDelay;

    private RetryPolicy(Builder builder) {
        this.maxRetries = builder.maxRetries;
        this.initialDelay = builder.initialDelay;
        this.multiplier = builder.multiplier;
        this.maxDelay = builder.maxDelay;
    }

    public static RetryPolicy defaultPolicy() {
        return builder().build();
    }

    public static RetryPolicy none() {
        return builder().maxRetries(0).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    int maxRetries() {
        return maxRetries;
    }

    // Computes delay using exponential backoff: initialDelay * multiplier^attempt
    Duration delayForAttempt(int attempt) {
        if (attempt <= 0) {
            return initialDelay;
        }
        long delayMs = (long) (initialDelay.toMillis() * Math.pow(multiplier, attempt));
        return Duration.ofMillis(Math.min(delayMs, maxDelay.toMillis()));
    }

    boolean isRetryable(Throwable cause) {
        if (maxRetries <= 0 || cause == null) {
            return false;
        }
        return switch (cause) {
            case java.io.IOException io -> true;
            case io.proxycheck.api.exception.ProxyCheckException pce ->
                    pce.hasHttpStatusCode() && pce.httpStatusCode() >= 500;
            default -> false;
        };
    }

    public static final class Builder {
        private int maxRetries = 3;
        private Duration initialDelay = Duration.ofMillis(500);
        private double multiplier = 2.0;
        private Duration maxDelay = Duration.ofSeconds(8);

        private Builder() {}

        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries must be >= 0");
            }
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialDelay(Duration initialDelay) {
            this.initialDelay = Objects.requireNonNull(initialDelay);
            return this;
        }

        public Builder multiplier(double multiplier) {
            if (multiplier < 1.0) {
                throw new IllegalArgumentException("multiplier must be >= 1.0");
            }
            this.multiplier = multiplier;
            return this;
        }

        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = Objects.requireNonNull(maxDelay);
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
