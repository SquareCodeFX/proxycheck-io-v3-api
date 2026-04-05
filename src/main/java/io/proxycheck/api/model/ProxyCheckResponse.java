package io.proxycheck.api.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Immutable representation of a proxycheck.io API response. Contains metadata
 * (status, message, node, query time) and two result maps: one for IP lookups
 * and one for email lookups, both keyed by the queried address.
 *
 * <p>Provides convenience methods for filtering (e.g., {@link #threatIps()},
 * {@link #disposableEmails()}), streaming ({@link #streamResults()}), and
 * pattern matching via the sealed {@link Result} hierarchy.
 *
 * <p>Instances are created by the client internals via {@link Builder} and are
 * safe for concurrent read access.
 */
public final class ProxyCheckResponse {

    private final ResponseStatus status;
    private final String message;
    private final String node;
    private final Integer queryTime;
    private final Map<String, IpResult> ipResults;
    private final Map<String, EmailResult> emailResults;

    // Shared singleton for whitelisted addresses and empty batch results
    private static final ProxyCheckResponse EMPTY = new Builder().build();

    /**
     * Returns a shared empty response singleton. Used for whitelisted addresses
     * and empty batch results to avoid unnecessary object allocation.
     */
    public static ProxyCheckResponse empty() {
        return EMPTY;
    }

    private ProxyCheckResponse(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.node = builder.node;
        this.queryTime = builder.queryTime;
        this.ipResults = Map.copyOf(builder.ipResults);
        this.emailResults = Map.copyOf(builder.emailResults);
    }

    /** Returns the API response status (OK, WARNING, DENIED, or ERROR). */
    public ResponseStatus status() {
        return status;
    }

    /** Returns the human-readable status message from the API, or {@code null}. */
    public String message() {
        return message;
    }

    /** Returns the cluster node identifier that served this request, or {@code null}. */
    public String node() {
        return node;
    }

    /** Returns the server-side query processing time in milliseconds, or {@code null}. */
    public Integer queryTime() {
        return queryTime;
    }

    /** Returns an unmodifiable map of IP address results keyed by the queried address. */
    public Map<String, IpResult> ipResults() {
        return ipResults;
    }

    /** Returns an unmodifiable map of email results keyed by the queried address. */
    public Map<String, EmailResult> emailResults() {
        return emailResults;
    }

    /** Returns the first IP result, useful for single-address queries. */
    public Optional<IpResult> firstIpResult() {
        return ipResults.values().stream().findFirst();
    }

    /** Returns the first email result, useful for single-address queries. */
    public Optional<EmailResult> firstEmailResult() {
        return emailResults.values().stream().findFirst();
    }

    /** Looks up an IP result by its exact address string. */
    public Optional<IpResult> ipResult(String address) {
        return Optional.ofNullable(ipResults.get(address));
    }

    /** Looks up an email result by its exact address string. */
    public Optional<EmailResult> emailResult(String address) {
        return Optional.ofNullable(emailResults.get(address));
    }

    /**
     * Unified lookup across both IP and email results by address. Checks IP
     * results first since they are more common in typical usage.
     */
    public Optional<Result> result(String address) {
        var ip = ipResults.get(address);
        if (ip != null) return Optional.of(ip);
        return Optional.ofNullable(emailResults.get(address));
    }

    /**
     * Returns a combined stream of all results (IP and email) as the sealed
     * {@link Result} type, enabling exhaustive pattern matching with {@code switch}.
     */
    // Explicit casts needed because Stream.concat requires the same generic type
    public Stream<Result> streamResults() {
        return Stream.concat(
                ipResults.values().stream().map(r -> (Result) r),
                emailResults.values().stream().map(r -> (Result) r));
    }

    /**
     * Returns IP results that match the given predicate. Useful for custom
     * filtering (e.g., by network type, risk level, or country).
     */
    public List<IpResult> ipResultsMatching(Predicate<IpResult> predicate) {
        return ipResults.values().stream().filter(predicate).toList();
    }

    /** Returns the total number of results across both IP and email lookups. */
    public int totalResults() {
        return ipResults.size() + emailResults.size();
    }

    /** Returns all IP results flagged as a threat (proxy, VPN, TOR, compromised, or scraper). */
    public List<IpResult> threatIps() {
        return ipResults.values().stream()
                .filter(IpResult::isThreat)
                .toList();
    }

    /** Returns all IP results that are not flagged as a threat. */
    public List<IpResult> safeIps() {
        return ipResults.values().stream()
                .filter(ip -> !ip.isThreat())
                .toList();
    }

    /** Returns all email results identified as disposable (throwaway) addresses. */
    public List<EmailResult> disposableEmails() {
        return emailResults.values().stream()
                .filter(EmailResult::isDisposable)
                .toList();
    }

    /** Returns all email results that are not disposable. */
    public List<EmailResult> legitimateEmails() {
        return emailResults.values().stream()
                .filter(e -> !e.isDisposable())
                .toList();
    }

    /** Returns a stream over all IP results for functional-style processing. */
    public Stream<IpResult> streamIpResults() {
        return ipResults.values().stream();
    }

    /** Returns a stream over all email results for functional-style processing. */
    public Stream<EmailResult> streamEmailResults() {
        return emailResults.values().stream();
    }

    /** Returns {@code true} if any IP result in this response is flagged as a threat. */
    public boolean hasThreat() {
        return ipResults.values().stream().anyMatch(IpResult::isThreat);
    }

    /** Returns {@code true} if any email result in this response is disposable. */
    public boolean hasDisposableEmail() {
        return emailResults.values().stream().anyMatch(EmailResult::isDisposable);
    }

    /** Returns {@code true} if the response status is {@link ResponseStatus#OK}. */
    public boolean isOk() {
        return status == ResponseStatus.OK;
    }

    /** Returns {@code true} if the response status is {@link ResponseStatus#WARNING}. */
    public boolean isWarning() {
        return status == ResponseStatus.WARNING;
    }

    /** Returns {@code true} if the response status is {@link ResponseStatus#DENIED}. */
    public boolean isDenied() {
        return status == ResponseStatus.DENIED;
    }

    /** Returns {@code true} if the response status is {@link ResponseStatus#ERROR}. */
    public boolean isError() {
        return status == ResponseStatus.ERROR;
    }

    /** Returns {@code true} if the status is OK or WARNING (i.e., results are usable). */
    public boolean isSuccessful() {
        return status != null && status.isSuccessful();
    }

    /**
     * Attempts to resolve the API message string into a known {@link StatusMessage}
     * enum constant. Returns {@code null} if the message is unrecognized.
     */
    public StatusMessage statusMessage() {
        return StatusMessage.fromMessage(message);
    }

    @Override
    public String toString() {
        return "ProxyCheckResponse{status=%s, message='%s', node='%s', queryTime=%s, ipResults=%d, emailResults=%d}"
                .formatted(status, message, node, queryTime, ipResults.size(), emailResults.size());
    }

    /**
     * Mutable builder for constructing {@link ProxyCheckResponse} instances.
     * Used internally by the response parser and for merging batch results.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        private ResponseStatus status;
        private String message;
        private String node;
        private Integer queryTime;
        private final Map<String, IpResult> ipResults = new LinkedHashMap<>();
        private final Map<String, EmailResult> emailResults = new LinkedHashMap<>();

        public Builder status(ResponseStatus status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder node(String node) {
            this.node = node;
            return this;
        }

        public Builder queryTime(Integer queryTime) {
            this.queryTime = queryTime;
            return this;
        }

        public Builder addIpResult(String address, IpResult result) {
            this.ipResults.put(address, result);
            return this;
        }

        public Builder addEmailResult(String address, EmailResult result) {
            this.emailResults.put(address, result);
            return this;
        }

        public ProxyCheckResponse build() {
            return new ProxyCheckResponse(this);
        }
    }
}
