package io.proxycheck.api;

import io.proxycheck.api.model.ProxyCheckResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ProxyCheckClientTest {

    @Test
    void builderRequiresApiKey() {
        assertThrows(NullPointerException.class, () -> ProxyCheckClient.builder().build());
    }

    @Test
    void factoryMethod() {
        try (var client = ProxyCheckClient.of("test-key")) {
            assertNotNull(client);
        }
    }

    @Test
    void builderCreatesClient() {
        try (var client = ProxyCheckClient.builder().apiKey("test-key").noRetry().build()) {
            assertNotNull(client);
        }
    }

    @Test
    void builderWithTimeout() {
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .timeout(Duration.ofSeconds(10))
                .noRetry()
                .build()) {
            assertNotNull(client);
        }
    }

    @Test
    void builderWithCache() {
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .cache(Duration.ofMinutes(5))
                .noRetry()
                .build()) {
            assertNotNull(client);
        }
    }

    @Test
    void builderWithCacheAndMaxSize() {
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .cache(Duration.ofMinutes(5), 500)
                .noRetry()
                .build()) {
            assertNotNull(client);
        }
    }

    @Test
    void builderWithRateLimit() {
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .rateLimitPerSecond(100)
                .noRetry()
                .build()) {
            assertNotNull(client);
        }
    }

    @Test
    void builderWithRetryPolicy() {
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .retryPolicy(RetryPolicy.builder().maxRetries(5).build())
                .build()) {
            assertNotNull(client);
        }
    }

    @Test
    void builderWithNoRetry() {
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .noRetry()
                .build()) {
            assertNotNull(client);
        }
    }

    @Test
    void checkRejectsNull() {
        try (var client = ProxyCheckClient.builder().apiKey("test-key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.check((String) null));
        }
    }

    @Test
    void checkCollectionRejectsEmpty() {
        try (var client = ProxyCheckClient.builder().apiKey("test-key").noRetry().build()) {
            assertThrows(IllegalArgumentException.class, () -> client.check(List.of()));
        }
    }

    @Test
    void checkAsyncRejectsNull() {
        try (var client = ProxyCheckClient.builder().apiKey("test-key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.checkAsync((String) null));
        }
    }

    @Test
    void checkAsyncCollectionRejectsEmpty() {
        try (var client = ProxyCheckClient.builder().apiKey("test-key").noRetry().build()) {
            assertThrows(IllegalArgumentException.class, () -> client.checkAsync(List.of()));
        }
    }

    @Test
    void checkMultipleRejectsEmpty() {
        try (var client = ProxyCheckClient.builder().apiKey("test-key").noRetry().build()) {
            assertThrows(IllegalArgumentException.class, () -> client.checkMultiple());
        }
    }

    @Test
    void checkMultipleRejectsNull() {
        try (var client = ProxyCheckClient.builder().apiKey("test-key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.checkMultiple((String[]) null));
        }
    }

    @Test
    void cacheInvalidateAndClearDoNotThrowWithoutCache() {
        try (var client = ProxyCheckClient.builder().apiKey("test-key").noRetry().build()) {
            assertDoesNotThrow(() -> client.invalidateCache("1.2.3.4"));
            assertDoesNotThrow(client::clearCache);
        }
    }

    @Test
    void builderRejectsInvalidCacheMaxSize() {
        assertThrows(IllegalArgumentException.class, () ->
                ProxyCheckClient.builder().apiKey("test-key").cache(Duration.ofMinutes(1), 0));
    }

    @Test
    void builderRejectsInvalidRateLimit() {
        assertThrows(IllegalArgumentException.class, () ->
                ProxyCheckClient.builder().apiKey("test-key").rateLimitPerSecond(0));
    }

    @Test
    void whitelistSkipsCheck() {
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .noRetry()
                .whitelist("127.0.0.1", "10.0.0.1")
                .build()) {
            var response = client.check("127.0.0.1");
            assertSame(ProxyCheckResponse.empty(), response);
        }
    }

    @Test
    void whitelistSkipsAsyncCheck() throws Exception {
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .noRetry()
                .whitelist("127.0.0.1")
                .build()) {
            var response = client.checkAsync("127.0.0.1").get();
            assertSame(ProxyCheckResponse.empty(), response);
        }
    }

    @Test
    void whitelistSkipsCollectionCheck() {
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .noRetry()
                .whitelist("1.1.1.1", "2.2.2.2")
                .build()) {
            var response = client.check(List.of("1.1.1.1", "2.2.2.2"));
            assertSame(ProxyCheckResponse.empty(), response);
        }
    }

    @Test
    void whitelistFromCollection() {
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .noRetry()
                .whitelist(List.of("::1"))
                .build()) {
            var response = client.check("::1");
            assertSame(ProxyCheckResponse.empty(), response);
        }
    }

    @Test
    void listenerCanBeAddedAndRemoved() {
        var listener = new CheckListener() {};
        try (var client = ProxyCheckClient.builder().apiKey("test-key").noRetry().build()) {
            client.addListener(listener);
            assertTrue(client.removeListener(listener));
            assertFalse(client.removeListener(listener));
        }
    }

    @Test
    void fluentListenerInBuilder() {
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .noRetry()
                .listener(CheckListener.builder()
                        .onRequest(addrs -> {})
                        .onCacheHit(addr -> {})
                        .build())
                .build()) {
            assertNotNull(client);
        }
    }

    @Test
    void listenerBuilderIntegration() {
        var called = new AtomicBoolean(false);
        try (var client = ProxyCheckClient.builder()
                .apiKey("test-key")
                .noRetry()
                .whitelist("1.2.3.4")
                .listener(new CheckListener() {
                    @Override
                    public void onCacheHit(String address) {
                        called.set(true);
                    }
                })
                .build()) {
            assertNotNull(client);
        }
    }
}
