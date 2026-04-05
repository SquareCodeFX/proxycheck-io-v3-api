package io.proxycheck.api.exception;

/**
 * Unchecked exception thrown for all API client errors. Optionally carries the
 * HTTP status code when the failure originated from a server response (5xx).
 * Use {@link #hasHttpStatusCode()} to check before accessing the code.
 */
public class ProxyCheckException extends RuntimeException {

    // 0 means no HTTP status (e.g., network error, interrupt, parse failure)
    private final int httpStatusCode;

    public ProxyCheckException(String message) {
        this(message, 0, null);
    }

    public ProxyCheckException(String message, Throwable cause) {
        this(message, 0, cause);
    }

    public ProxyCheckException(String message, int httpStatusCode) {
        this(message, httpStatusCode, null);
    }

    public ProxyCheckException(String message, int httpStatusCode, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    public int httpStatusCode() {
        return httpStatusCode;
    }

    public boolean hasHttpStatusCode() {
        return httpStatusCode > 0;
    }
}
