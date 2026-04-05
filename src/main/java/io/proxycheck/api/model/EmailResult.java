package io.proxycheck.api.model;

/**
 * Result for a single email address check. The primary data point is whether
 * the email belongs to a disposable (throwaway) mail service.
 *
 * @param address    the queried email address
 * @param disposable raw API value; may be {@code null} if the field was omitted
 */
public record EmailResult(
        String address,
        Boolean disposable
) implements Result {

    /**
     * Returns {@code true} if this email is from a known disposable mail provider.
     * Treats {@code null} as {@code false} since the API may omit the field.
     */
    public boolean isDisposable() {
        return Boolean.TRUE.equals(disposable);
    }
}
