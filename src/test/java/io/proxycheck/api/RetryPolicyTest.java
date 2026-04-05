package io.proxycheck.api;

import io.proxycheck.api.exception.ProxyCheckException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    @Test
    void defaultPolicy() {
        var policy = RetryPolicy.defaultPolicy();
        assertEquals(3, policy.maxRetries());
    }

    @Test
    void nonePolicy() {
        var policy = RetryPolicy.none();
        assertEquals(0, policy.maxRetries());
    }

    @Test
    void exponentialBackoffDelays() {
        var policy = RetryPolicy.builder()
                .initialDelay(Duration.ofMillis(100))
                .multiplier(2.0)
                .build();

        assertEquals(100, policy.delayForAttempt(0).toMillis());
        assertEquals(200, policy.delayForAttempt(1).toMillis());
        assertEquals(400, policy.delayForAttempt(2).toMillis());
    }

    @Test
    void delayCappedAtMaxDelay() {
        var policy = RetryPolicy.builder()
                .initialDelay(Duration.ofSeconds(1))
                .multiplier(10.0)
                .maxDelay(Duration.ofSeconds(5))
                .build();

        assertEquals(5000, policy.delayForAttempt(2).toMillis());
    }

    @Test
    void isRetryableForIOException() {
        var policy = RetryPolicy.defaultPolicy();
        assertTrue(policy.isRetryable(new IOException("timeout")));
    }

    @Test
    void isRetryableForServerError() {
        var policy = RetryPolicy.defaultPolicy();
        assertTrue(policy.isRetryable(new ProxyCheckException("Server error", 500)));
        assertTrue(policy.isRetryable(new ProxyCheckException("Server error", 502)));
    }

    @Test
    void notRetryableForClientError() {
        var policy = RetryPolicy.defaultPolicy();
        assertFalse(policy.isRetryable(new ProxyCheckException("Bad request", 400)));
    }

    @Test
    void notRetryableForNoStatusCode() {
        var policy = RetryPolicy.defaultPolicy();
        assertFalse(policy.isRetryable(new ProxyCheckException("parse error")));
    }

    @Test
    void notRetryableWhenDisabled() {
        var policy = RetryPolicy.none();
        assertFalse(policy.isRetryable(new IOException("timeout")));
    }

    @Test
    void isRetryableForAll5xx() {
        var policy = RetryPolicy.defaultPolicy();
        assertTrue(policy.isRetryable(new ProxyCheckException("error", 503)));
        assertTrue(policy.isRetryable(new ProxyCheckException("error", 504)));
        assertTrue(policy.isRetryable(new ProxyCheckException("error", 599)));
    }

    @Test
    void isRetryableWithNull() {
        var policy = RetryPolicy.defaultPolicy();
        assertFalse(policy.isRetryable(null));
    }

    @Test
    void isRetryableWithGenericException() {
        var policy = RetryPolicy.defaultPolicy();
        assertFalse(policy.isRetryable(new RuntimeException("unrelated")));
    }

    @Test
    void builderRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () ->
                RetryPolicy.builder().maxRetries(-1));
        assertThrows(IllegalArgumentException.class, () ->
                RetryPolicy.builder().multiplier(0.5));
    }
}
