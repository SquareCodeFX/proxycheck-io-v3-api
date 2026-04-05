package io.proxycheck.api;

import io.proxycheck.api.model.ProxyCheckResponse;
import io.proxycheck.api.model.ResponseStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ResponseCacheTest {

    private static ProxyCheckResponse okResponse() {
        return new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .build();
    }

    @Test
    void putAndGet() {
        var cache = new ResponseCache(Duration.ofMinutes(5), 100);
        var response = okResponse();

        cache.put("1.2.3.4", response);

        assertTrue(cache.get("1.2.3.4").isPresent());
        assertEquals(response, cache.get("1.2.3.4").get());
    }

    @Test
    void getMissReturnsEmpty() {
        var cache = new ResponseCache(Duration.ofMinutes(5), 100);
        assertTrue(cache.get("1.2.3.4").isEmpty());
    }

    @Test
    void expiredEntryReturnsEmpty() throws InterruptedException {
        var cache = new ResponseCache(Duration.ofMillis(50), 100);
        cache.put("1.2.3.4", okResponse());

        Thread.sleep(80);

        assertTrue(cache.get("1.2.3.4").isEmpty());
    }

    @Test
    void invalidateRemovesEntry() {
        var cache = new ResponseCache(Duration.ofMinutes(5), 100);
        cache.put("1.2.3.4", okResponse());

        cache.invalidate("1.2.3.4");

        assertTrue(cache.get("1.2.3.4").isEmpty());
    }

    @Test
    void clearRemovesAllEntries() {
        var cache = new ResponseCache(Duration.ofMinutes(5), 100);
        cache.put("1.2.3.4", okResponse());
        cache.put("5.6.7.8", okResponse());

        cache.clear();

        assertEquals(0, cache.size());
    }

    @Test
    void maxSizeEvictsOldest() {
        var cache = new ResponseCache(Duration.ofMinutes(5), 2);
        cache.put("first", okResponse());
        cache.put("second", okResponse());
        cache.put("third", okResponse());

        assertEquals(2, cache.size());
        assertTrue(cache.get("first").isEmpty());
        assertTrue(cache.get("second").isPresent());
        assertTrue(cache.get("third").isPresent());
    }

    @Test
    void sizeReflectsEntryCount() {
        var cache = new ResponseCache(Duration.ofMinutes(5), 100);
        assertEquals(0, cache.size());

        cache.put("a", okResponse());
        assertEquals(1, cache.size());

        cache.put("b", okResponse());
        assertEquals(2, cache.size());
    }
}
