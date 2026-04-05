package io.proxycheck.api;

import io.proxycheck.api.exception.ProxyCheckException;
import io.proxycheck.api.model.ProxyCheckResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * HTTP client for the proxycheck.io v3 API. Supports synchronous and asynchronous
 * IP/email checks with optional caching, rate limiting, retry, and whitelist features.
 *
 * <p>Use {@link #of(String)} for a quick default client or {@link #builder()} for
 * full configuration. Implements {@link AutoCloseable} to release the underlying
 * {@link HttpClient} resources.
 */
public final class ProxyCheckClient implements AutoCloseable {

    private static final String BASE_URL = "https://proxycheck.io/v3/";
    private static final String USER_AGENT = "proxycheck-java/1.0.0";

    /**
     * Maximum number of addresses per single API request during the v3 beta.
     */
    static final int MAX_BATCH_SIZE = 1000;

    private final String apiKey;
    private final HttpClient httpClient;
    private final Duration timeout;
    private final ResponseCache cache;
    private final RateLimiter rateLimiter;
    private final RetryPolicy retryPolicy;
    private final Set<String> whitelist;
    // CopyOnWriteArrayList allows safe iteration while listeners are added/removed concurrently
    private final List<CheckListener> listeners;

    private ProxyCheckClient(Builder builder) {
        this.apiKey = builder.apiKey;
        this.timeout = builder.timeout;
        // Fall back to a default HttpClient with HTTP/2 if none was provided
        this.httpClient = builder.httpClient != null
                ? builder.httpClient
                : HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(builder.timeout)
                    .build();
        this.cache = builder.cacheTtl != null
                ? new ResponseCache(builder.cacheTtl, builder.cacheMaxSize)
                : null;
        this.rateLimiter = builder.maxRequestsPerSecond > 0
                ? new RateLimiter(builder.maxRequestsPerSecond)
                : null;
        this.retryPolicy = builder.retryPolicy;
        // Use Set.of() for empty case to avoid wrapping overhead
        this.whitelist = builder.whitelist.isEmpty()
                ? Set.of()
                : Set.copyOf(builder.whitelist);
        this.listeners = new CopyOnWriteArrayList<>(builder.listeners);
    }

    /**
     * Creates a client with the given API key and default settings (30s timeout,
     * 3 retries with exponential backoff, no cache, no rate limit).
     *
     * @param apiKey your proxycheck.io API key
     * @return a ready-to-use client instance
     */
    public static ProxyCheckClient of(String apiKey) {
        return builder().apiKey(apiKey).build();
    }

    /**
     * Returns a new {@link Builder} for fine-grained client configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Registers a listener to observe request lifecycle events. Listeners are
     * notified in registration order and can be added at any time (thread-safe).
     *
     * @param listener the listener to register (must not be null)
     */
    public void addListener(CheckListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     * @return {@code true} if the listener was found and removed
     */
    public boolean removeListener(CheckListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Checks a single IP or email address with default query flags.
     *
     * @param address the IP address or email to check
     * @return the API response containing the check result
     * @throws ProxyCheckException on API or network errors
     */
    public ProxyCheckResponse check(String address) {
        return check(address, null);
    }

    /**
     * Checks a single IP or email address with the specified query flags.
     * Returns immediately for whitelisted or cached addresses without making
     * an API call. Uses HTTP GET for single-address lookups.
     *
     * @param address the IP address or email to check
     * @param flags   optional query parameters, or {@code null} for defaults
     * @return the API response containing the check result
     * @throws ProxyCheckException on API or network errors
     */
    public ProxyCheckResponse check(String address, QueryFlags flags) {
        Objects.requireNonNull(address, "address must not be null");
        // Whitelisted addresses return immediately without an API call
        if (isWhitelisted(address)) {
            return ProxyCheckResponse.empty();
        }
        var cached = getFromCache(address, flags);
        if (cached != null) {
            fireOnCacheHit(address);
            return cached;
        }
        fireOnRequest(List.of(address));
        try {
            // Single address uses GET; batch uses POST (see check(Collection) below)
            String json = executeWithRetry(buildGetRequest(address, flags));
            var response = parseResponse(json, flags);
            putInCache(address, flags, response);
            fireOnResponse(List.of(address), response);
            return response;
        } catch (ProxyCheckException e) {
            fireOnError(List.of(address), e);
            throw e;
        }
    }

    /**
     * Batch-checks multiple addresses with default query flags. Uses HTTP POST
     * and automatically splits into batches of {@value #MAX_BATCH_SIZE} if needed.
     *
     * @param addresses the IP addresses and/or emails to check (must not be empty)
     * @return a combined response containing all results
     * @throws IllegalArgumentException if the collection is empty
     * @throws ProxyCheckException      on API or network errors
     */
    public ProxyCheckResponse check(Collection<String> addresses) {
        return check(addresses, null);
    }

    /**
     * Batch-checks multiple addresses with the specified query flags. Whitelisted
     * addresses are filtered out before the API call. If only one address remains
     * after filtering, it falls back to a single GET request for efficiency.
     *
     * @param addresses the IP addresses and/or emails to check (must not be empty)
     * @param flags     optional query parameters, or {@code null} for defaults
     * @return a combined response containing all results
     * @throws IllegalArgumentException if the collection is empty
     * @throws ProxyCheckException      on API or network errors
     */
    public ProxyCheckResponse check(Collection<String> addresses, QueryFlags flags) {
        Objects.requireNonNull(addresses, "addresses must not be null");
        if (addresses.isEmpty()) {
            throw new IllegalArgumentException("addresses must not be empty");
        }
        // Remove whitelisted entries before making the API call
        var filtered = filterWhitelisted(addresses);
        if (filtered.isEmpty()) {
            return ProxyCheckResponse.empty();
        }
        // Optimize: single remaining address uses GET instead of POST
        if (filtered.size() == 1) {
            return check(filtered.iterator().next(), flags);
        }
        // Auto-split into batches of MAX_BATCH_SIZE to respect the API limit
        if (filtered.size() > MAX_BATCH_SIZE) {
            return checkInBatches(new ArrayList<>(filtered), flags);
        }
        fireOnRequest(filtered);
        try {
            String json = executeWithRetry(buildPostRequest(filtered, flags));
            var response = parseResponse(json, flags);
            cacheIndividualResults(response, flags);
            fireOnResponse(filtered, response);
            return response;
        } catch (ProxyCheckException e) {
            fireOnError(filtered, e);
            throw e;
        }
    }

    private ProxyCheckResponse checkInBatches(List<String> addresses, QueryFlags flags) {
        var combinedBuilder = new ProxyCheckResponse.Builder();
        for (int i = 0; i < addresses.size(); i += MAX_BATCH_SIZE) {
            var batch = addresses.subList(i, Math.min(i + MAX_BATCH_SIZE, addresses.size()));
            var batchResponse = checkSingleBatch(batch, flags);
            // Merge results; status and metadata come from the last batch
            if (batchResponse.status() != null) {
                combinedBuilder.status(batchResponse.status());
            }
            if (batchResponse.message() != null) {
                combinedBuilder.message(batchResponse.message());
            }
            if (batchResponse.node() != null) {
                combinedBuilder.node(batchResponse.node());
            }
            batchResponse.ipResults().forEach(combinedBuilder::addIpResult);
            batchResponse.emailResults().forEach(combinedBuilder::addEmailResult);
        }
        return combinedBuilder.build();
    }

    private ProxyCheckResponse checkSingleBatch(List<String> batch, QueryFlags flags) {
        fireOnRequest(batch);
        try {
            String json = executeWithRetry(buildPostRequest(batch, flags));
            var response = parseResponse(json, flags);
            cacheIndividualResults(response, flags);
            fireOnResponse(batch, response);
            return response;
        } catch (ProxyCheckException e) {
            fireOnError(batch, e);
            throw e;
        }
    }

    /**
     * Varargs shorthand for {@link #check(Collection)} when addresses are known
     * at compile time.
     */
    public ProxyCheckResponse checkMultiple(String... addresses) {
        return checkMultiple(null, addresses);
    }

    /**
     * Varargs shorthand for {@link #check(Collection, QueryFlags)} when addresses
     * are known at compile time.
     */
    public ProxyCheckResponse checkMultiple(QueryFlags flags, String... addresses) {
        Objects.requireNonNull(addresses, "addresses must not be null");
        return check(List.of(addresses), flags);
    }

    /**
     * Asynchronously checks a single address with default query flags.
     * Returns a completed future immediately for whitelisted or cached addresses.
     *
     * @param address the IP address or email to check
     * @return a future that completes with the API response
     */
    public CompletableFuture<ProxyCheckResponse> checkAsync(String address) {
        return checkAsync(address, null);
    }

    /**
     * Asynchronously checks a single address with the specified query flags.
     * Uses the {@link HttpClient}'s async pipeline and applies retry logic
     * without blocking the calling thread.
     *
     * @param address the IP address or email to check
     * @param flags   optional query parameters, or {@code null} for defaults
     * @return a future that completes with the API response
     */
    public CompletableFuture<ProxyCheckResponse> checkAsync(String address, QueryFlags flags) {
        Objects.requireNonNull(address, "address must not be null");
        if (isWhitelisted(address)) {
            return CompletableFuture.completedFuture(ProxyCheckResponse.empty());
        }
        var cached = getFromCache(address, flags);
        if (cached != null) {
            fireOnCacheHit(address);
            return CompletableFuture.completedFuture(cached);
        }
        fireOnRequest(List.of(address));
        return executeAsyncWithRetry(buildGetRequest(address, flags), 0)
                .thenApply(json -> {
                    var response = parseResponse(json, flags);
                    putInCache(address, flags, response);
                    fireOnResponse(List.of(address), response);
                    return response;
                });
    }

    /**
     * Asynchronously batch-checks multiple addresses with default query flags.
     *
     * @param addresses the IP addresses and/or emails to check (must not be empty)
     * @return a future that completes with the combined API response
     */
    public CompletableFuture<ProxyCheckResponse> checkAsync(Collection<String> addresses) {
        return checkAsync(addresses, null);
    }

    /**
     * Asynchronously batch-checks multiple addresses with the specified query flags.
     * Falls back to a single async GET if only one non-whitelisted address remains.
     *
     * @param addresses the IP addresses and/or emails to check (must not be empty)
     * @param flags     optional query parameters, or {@code null} for defaults
     * @return a future that completes with the combined API response
     */
    public CompletableFuture<ProxyCheckResponse> checkAsync(Collection<String> addresses, QueryFlags flags) {
        Objects.requireNonNull(addresses, "addresses must not be null");
        if (addresses.isEmpty()) {
            throw new IllegalArgumentException("addresses must not be empty");
        }
        var filtered = filterWhitelisted(addresses);
        if (filtered.isEmpty()) {
            return CompletableFuture.completedFuture(ProxyCheckResponse.empty());
        }
        if (filtered.size() == 1) {
            return checkAsync(filtered.iterator().next(), flags);
        }
        fireOnRequest(filtered);
        return executeAsyncWithRetry(buildPostRequest(filtered, flags), 0)
                .thenApply(json -> {
                    var response = parseResponse(json, flags);
                    cacheIndividualResults(response, flags);
                    fireOnResponse(filtered, response);
                    return response;
                });
    }

    /**
     * Returns a {@link DashboardClient} that shares this client's HTTP connection
     * pool, timeout, and retry policy. Dashboard calls are free and do not count
     * against the regular query quota.
     *
     * <p>The returned client is not owned by this instance and will not be closed
     * when this client is closed. Closing it explicitly is a no-op.
     *
     * @return a dashboard client configured with this client's settings
     */
    public DashboardClient dashboard() {
        return DashboardClient.from(apiKey, httpClient, timeout, retryPolicy);
    }

    /**
     * Removes a specific address from the response cache. No-op if caching is
     * not enabled or the address is not cached.
     *
     * @param address the address whose cached response should be evicted
     */
    public void invalidateCache(String address) {
        if (cache != null) {
            cache.invalidate(address);
        }
    }

    /**
     * Clears all entries from the response cache. No-op if caching is not enabled.
     */
    public void clearCache() {
        if (cache != null) {
            cache.clear();
        }
    }

    private boolean isWhitelisted(String address) {
        return !whitelist.isEmpty() && whitelist.contains(address);
    }

    private Collection<String> filterWhitelisted(Collection<String> addresses) {
        if (whitelist.isEmpty()) {
            return addresses;
        }
        return addresses.stream()
                .filter(a -> !whitelist.contains(a))
                .toList();
    }

    private HttpRequest buildGetRequest(String address, QueryFlags flags) {
        return HttpRequest.newBuilder()
                .uri(URI.create(buildUrl(address, flags)))
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
    }

    private HttpRequest buildPostRequest(Collection<String> addresses, QueryFlags flags) {
        // The API expects raw addresses separated by commas: ips=8.8.8.8,user@example.com
        // Commas are the separator and must NOT be encoded. Addresses are sent as-is
        // since IPs contain no special characters and the @ in emails must remain literal.
        String postData = "ips=" + String.join(",", addresses);
        return HttpRequest.newBuilder()
                .uri(URI.create(buildUrl(null, flags)))
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postData))
                .build();
    }

    private String buildUrl(String address, QueryFlags flags) {
        var sb = new StringBuilder(BASE_URL);
        if (address != null) {
            sb.append(URLEncoder.encode(address, StandardCharsets.UTF_8));
        }
        sb.append("?key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        if (flags != null) {
            sb.append(flags.toQueryString());
        }
        return sb.toString();
    }

    /**
     * Executes a request with exponential backoff retry. On failure, checks both
     * the exception itself and its cause for retryability (e.g., an IOException
     * wrapped inside a ProxyCheckException).
     */
    private String executeWithRetry(HttpRequest request) {
        ProxyCheckException lastException = null;
        int maxAttempts = 1 + retryPolicy.maxRetries();
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                if (attempt > 0) {
                    fireOnRetry(attempt, lastException);
                    // attempt-1 because delayForAttempt(0) = initial delay
                    Duration delay = retryPolicy.delayForAttempt(attempt - 1);
                    Thread.sleep(delay.toMillis());
                }
                return execute(request);
            } catch (ProxyCheckException e) {
                lastException = e;
                // Check both the exception and its cause (e.g., IOException wrapped in ProxyCheckException)
                if (!retryPolicy.isRetryable(e) && !retryPolicy.isRetryable(e.getCause())) {
                    throw e;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProxyCheckException("Retry interrupted", e);
            }
        }
        // All retries exhausted, throw the last captured exception
        assert lastException != null;
        throw lastException;
    }

    private String execute(HttpRequest request) {
        acquireRateLimit();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            validateStatusCode(response);
            return response.body();
        } catch (IOException e) {
            throw new ProxyCheckException("HTTP request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProxyCheckException("HTTP request interrupted", e);
        }
    }

    private CompletableFuture<String> executeAsyncWithRetry(HttpRequest request, int attempt) {
        // Async uses tryAcquire (non-blocking) instead of acquire (blocking)
        // to avoid blocking the calling thread; fails fast if no permits available
        if (rateLimiter != null && !rateLimiter.tryAcquire()) {
            return CompletableFuture.failedFuture(
                    new ProxyCheckException("Rate limit exceeded"));
        }
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    validateStatusCode(response);
                    return response.body();
                })
                .exceptionallyCompose(ex -> {
                    Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                    if (attempt < retryPolicy.maxRetries()
                            && (retryPolicy.isRetryable(cause)
                                || (cause instanceof ProxyCheckException pce && retryPolicy.isRetryable(pce.getCause())))) {
                        fireOnRetry(attempt + 1, cause instanceof Exception e ? e : new ProxyCheckException(cause.getMessage(), cause));
                        Duration delay = retryPolicy.delayForAttempt(attempt);
                        return CompletableFuture.supplyAsync(() -> null,
                                        CompletableFuture.delayedExecutor(delay.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS))
                                .thenCompose(ignored -> executeAsyncWithRetry(request, attempt + 1));
                    }
                    return CompletableFuture.failedFuture(cause);
                });
    }

    private void acquireRateLimit() {
        if (rateLimiter != null) {
            try {
                rateLimiter.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProxyCheckException("Rate limit acquire interrupted", e);
            }
        }
    }

    // Only 5xx errors are thrown here. 4xx responses are handled by the API
    // itself and returned as structured error responses (status=denied/error).
    private static void validateStatusCode(HttpResponse<String> response) {
        int code = response.statusCode();
        if (code >= 500) {
            throw new ProxyCheckException("Server error: HTTP " + code, code);
        }
    }

    private static ProxyCheckResponse parseResponse(String json, QueryFlags flags) {
        return ResponseParser.parse(json, flags != null && flags.isShort());
    }

    private ProxyCheckResponse getFromCache(String address, QueryFlags flags) {
        if (cache == null) {
            return null;
        }
        String key = cacheKey(address, flags);
        return cache.get(key).orElse(null);
    }

    // Only cache successful responses to avoid persisting transient errors
    private void putInCache(String address, QueryFlags flags, ProxyCheckResponse response) {
        if (cache != null && response.isSuccessful()) {
            cache.put(cacheKey(address, flags), response);
        }
    }

    // Cache each address result individually from a batch response so subsequent
    // single-address lookups can hit the cache without another API call
    private void cacheIndividualResults(ProxyCheckResponse response, QueryFlags flags) {
        if (cache == null || !response.isSuccessful()) {
            return;
        }
        var singleFlags = flags != null && flags.isShort() ? null : flags;
        response.ipResults().forEach((address, ipResult) -> {
            var single = new ProxyCheckResponse.Builder()
                    .status(response.status())
                    .addIpResult(address, ipResult)
                    .build();
            cache.put(cacheKey(address, singleFlags), single);
        });
        response.emailResults().forEach((address, emailResult) -> {
            var single = new ProxyCheckResponse.Builder()
                    .status(response.status())
                    .addEmailResult(address, emailResult)
                    .build();
            cache.put(cacheKey(address, singleFlags), single);
        });
    }

    // Cache key includes query flags so the same address with different flags
    // produces separate cache entries (e.g., short vs. detailed response)
    private static String cacheKey(String address, QueryFlags flags) {
        return flags != null ? address + "|" + flags.toQueryString() : address;
    }

    private void fireOnRequest(Collection<String> addresses) {
        for (var l : listeners) l.onRequest(addresses);
    }

    private void fireOnResponse(Collection<String> addresses, ProxyCheckResponse response) {
        for (var l : listeners) l.onResponse(addresses, response);
    }

    private void fireOnError(Collection<String> addresses, Exception error) {
        for (var l : listeners) l.onError(addresses, error);
    }

    private void fireOnCacheHit(String address) {
        for (var l : listeners) l.onCacheHit(address);
    }

    private void fireOnRetry(int attempt, Exception cause) {
        for (var l : listeners) l.onRetry(attempt, cause);
    }

    @Override
    public void close() {
        httpClient.close();
    }

    /**
     * Fluent builder for configuring a {@link ProxyCheckClient}. The only required
     * field is the API key; all others have sensible defaults.
     *
     * <p>Defaults:
     * <ul>
     *   <li>Timeout: 30 seconds</li>
     *   <li>Retry: 3 retries with exponential backoff (500ms initial, 2x multiplier, 8s cap)</li>
     *   <li>Cache: disabled</li>
     *   <li>Rate limit: disabled</li>
     * </ul>
     */
    public static final class Builder {
        private String apiKey;
        private Duration timeout = Duration.ofSeconds(30);
        private HttpClient httpClient;
        private Duration cacheTtl;
        private int cacheMaxSize = 1000;
        private int maxRequestsPerSecond;
        private RetryPolicy retryPolicy = RetryPolicy.defaultPolicy();
        private final Set<String> whitelist = new HashSet<>();
        private final List<CheckListener> listeners = new ArrayList<>();

        private Builder() {}

        /** Sets the proxycheck.io API key (required). */
        public Builder apiKey(String apiKey) {
            this.apiKey = Objects.requireNonNull(apiKey, "apiKey must not be null");
            return this;
        }

        /** Sets the HTTP request timeout. Defaults to 30 seconds. */
        public Builder timeout(Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
            return this;
        }

        /**
         * Provides a custom {@link HttpClient} instance. When set, the client
         * uses this instead of creating its own HTTP/2 client. Useful for
         * sharing a connection pool or configuring proxies/SSL.
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Enables response caching with the given TTL and a default max size of 1000 entries.
         *
         * @param ttl how long cached responses remain valid
         */
        public Builder cache(Duration ttl) {
            return cache(ttl, 1000);
        }

        /**
         * Enables response caching with the given TTL and maximum entry count.
         * Uses LRU eviction when the cache exceeds {@code maxSize}.
         *
         * @param ttl     how long cached responses remain valid
         * @param maxSize maximum number of entries before LRU eviction kicks in
         */
        public Builder cache(Duration ttl, int maxSize) {
            this.cacheTtl = Objects.requireNonNull(ttl, "ttl must not be null");
            if (maxSize < 1) {
                throw new IllegalArgumentException("maxSize must be >= 1");
            }
            this.cacheMaxSize = maxSize;
            return this;
        }

        /**
         * Enables client-side rate limiting using a token bucket that refills
         * once per second. Synchronous calls block until a permit is available;
         * async calls fail fast with a {@link ProxyCheckException}.
         *
         * @param maxRequestsPerSecond the maximum number of API requests per second
         */
        public Builder rateLimitPerSecond(int maxRequestsPerSecond) {
            if (maxRequestsPerSecond < 1) {
                throw new IllegalArgumentException("maxRequestsPerSecond must be >= 1");
            }
            this.maxRequestsPerSecond = maxRequestsPerSecond;
            return this;
        }

        /** Sets a custom retry policy for transient failures. */
        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy must not be null");
            return this;
        }

        /** Disables all retry attempts on failure. */
        public Builder noRetry() {
            this.retryPolicy = RetryPolicy.none();
            return this;
        }

        /**
         * Adds addresses to the client-side whitelist. Whitelisted addresses are
         * skipped entirely without making an API call, returning an empty response.
         */
        public Builder whitelist(String... addresses) {
            Collections.addAll(this.whitelist, addresses);
            return this;
        }

        /** Adds a collection of addresses to the client-side whitelist. */
        public Builder whitelist(Collection<String> addresses) {
            this.whitelist.addAll(addresses);
            return this;
        }

        /** Registers a {@link CheckListener} to observe request lifecycle events. */
        public Builder listener(CheckListener listener) {
            this.listeners.add(Objects.requireNonNull(listener));
            return this;
        }

        /**
         * Builds and returns a configured {@link ProxyCheckClient} instance.
         *
         * @throws NullPointerException if the API key has not been set
         */
        public ProxyCheckClient build() {
            Objects.requireNonNull(apiKey, "apiKey must be set");
            return new ProxyCheckClient(this);
        }
    }
}
