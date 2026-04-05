/**
 * Java client library for the proxycheck.io v3 API. Provides IP and email
 * threat intelligence lookups with built-in caching, rate limiting, retry
 * logic, and event-driven listener support.
 *
 * <p>Entry point: {@link io.proxycheck.api.ProxyCheckClient}.
 */
module io.proxycheck.api {
    requires java.net.http;
    requires com.google.gson;

    exports io.proxycheck.api;
    exports io.proxycheck.api.exception;
    exports io.proxycheck.api.model;

    // Gson needs reflective access to deserialize API JSON into model records
    opens io.proxycheck.api.model to com.google.gson;
}
