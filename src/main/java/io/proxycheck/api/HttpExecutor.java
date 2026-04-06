package io.proxycheck.api;

import io.proxycheck.api.exception.ProxyCheckException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Shared HTTP execution utilities: synchronous and asynchronous request dispatch
 * with exponential-backoff retry. Package-private; consumed by
 * {@link ProxyCheckClient} and {@link DashboardClient}.
 *
 * <p>All methods are static — this class holds no state and requires no instance.
 */
final class HttpExecutor {

    private HttpExecutor() {}

    /**
     * Executes {@code action} synchronously with exponential-backoff retry governed
     * by {@code policy}. Calls {@code onRetry} before each retry attempt (pass
     * {@code null} to skip the callback).
     *
     * <p>The action should throw {@link ProxyCheckException} on failure. Only
     * {@link java.io.IOException} causes and HTTP 5xx responses are retried;
     * all other failures propagate immediately.
     */
    static String executeWithRetry(RetryPolicy policy,
                                   Supplier<String> action,
                                   BiConsumer<Integer, Exception> onRetry) {
        ProxyCheckException lastException = null;
        int maxAttempts = 1 + policy.maxRetries();
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                if (attempt > 0) {
                    if (onRetry != null) onRetry.accept(attempt, lastException);
                    Thread.sleep(policy.delayForAttempt(attempt - 1).toMillis());
                }
                return action.get();
            } catch (ProxyCheckException e) {
                lastException = e;
                if (!policy.isRetryable(e) && !policy.isRetryable(e.getCause())) {
                    throw e;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProxyCheckException("Retry interrupted", e);
            }
        }
        assert lastException != null;
        throw lastException;
    }

    /**
     * Executes a single HTTP request synchronously, wrapping low-level errors in
     * {@link ProxyCheckException}. Throws on HTTP 5xx responses.
     */
    static String execute(HttpClient httpClient, HttpRequest request) {
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

    /**
     * Executes {@code action} asynchronously with exponential-backoff retry governed
     * by {@code policy}. The action is a function from attempt number to a future
     * string result. Calls {@code onRetry} before each retry (pass {@code null} to
     * skip the callback).
     *
     * <p>Rate limiting and other per-attempt setup should be handled inside
     * {@code action}; returning a failed future from {@code action} triggers the
     * retryability check just like a thrown exception.
     */
    static CompletableFuture<String> executeAsyncWithRetry(
            RetryPolicy policy,
            IntFunction<CompletableFuture<String>> action,
            int attempt,
            BiConsumer<Integer, Exception> onRetry) {
        return action.apply(attempt).exceptionallyCompose(ex -> {
            Throwable cause = ex instanceof CompletionException ce ? ce.getCause() : ex;
            if (attempt < policy.maxRetries()
                    && (policy.isRetryable(cause)
                        || (cause instanceof ProxyCheckException pce && policy.isRetryable(pce.getCause())))) {
                Exception retryEx = cause instanceof Exception e
                        ? e
                        : new ProxyCheckException(cause.getMessage(), cause);
                if (onRetry != null) onRetry.accept(attempt + 1, retryEx);
                Duration delay = policy.delayForAttempt(attempt);
                return CompletableFuture
                        .supplyAsync(() -> null,
                                CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS))
                        .thenCompose(ignored -> executeAsyncWithRetry(policy, action, attempt + 1, onRetry));
            }
            return CompletableFuture.failedFuture(cause);
        });
    }

    /** Throws {@link ProxyCheckException} for HTTP 5xx responses; no-op otherwise. */
    static void validateStatusCode(HttpResponse<String> response) {
        int code = response.statusCode();
        if (code >= 500) {
            throw new ProxyCheckException("Server error: HTTP " + code, code);
        }
    }
}
