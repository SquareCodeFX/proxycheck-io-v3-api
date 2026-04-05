package io.proxycheck.api;

import io.proxycheck.api.model.ProxyCheckResponse;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * TTL-based, size-bounded response cache using a synchronized {@link LinkedHashMap}
 * in access-order mode for O(1) LRU eviction.
 *
 * <p>Eviction is automatic: when the map exceeds {@code maxSize}, the least-recently
 * accessed entry is removed. Expired entries are removed on access (lazy eviction).
 */
final class ResponseCache {

    private final long ttlNanos;
    private final Map<String, CacheEntry> entries;

    ResponseCache(Duration ttl, int maxSize) {
        this.ttlNanos = ttl.toNanos();
        // accessOrder=true makes LinkedHashMap maintain LRU order on get()
        this.entries = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > maxSize || eldest.getValue().isExpired();
            }
        };
    }

    synchronized Optional<ProxyCheckResponse> get(String key) {
        var entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.response);
    }

    synchronized void put(String key, ProxyCheckResponse response) {
        entries.put(key, new CacheEntry(response, System.nanoTime() + ttlNanos));
    }

    synchronized void invalidate(String key) {
        entries.remove(key);
    }

    synchronized void clear() {
        entries.clear();
    }

    synchronized int size() {
        return entries.size();
    }

    private record CacheEntry(ProxyCheckResponse response, long expiresAtNano) {
        boolean isExpired() {
            return System.nanoTime() - expiresAtNano > 0;
        }
    }
}
