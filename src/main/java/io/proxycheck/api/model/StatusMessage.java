package io.proxycheck.api.model;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Known API status messages mapped to their expected {@link ResponseStatus}.
 * Covers warning, denied, and error conditions as documented in the
 * proxycheck.io API reference.
 *
 * <p>Use {@link #fromMessage(String)} for O(1) case-insensitive lookup by the
 * raw message string returned in the JSON response.
 */
public enum StatusMessage {

    // Warning messages
    API_KEY_DISABLED(ResponseStatus.WARNING,
            "Your API Key has been disabled for a violation of our terms of service."),
    NEAR_QUERY_LIMIT(ResponseStatus.WARNING,
            "You are within 10% of your query limit for the day."),
    NEAR_QUERY_LIMIT_BURST_CONSUMED(ResponseStatus.WARNING,
            "You are within 10% of your query limit for the day and a burst token has already been consumed."),
    OVER_QUERY_LIMIT_BURST_CONSUMED(ResponseStatus.WARNING,
            "You have gone over your daily query allowance and a burst token has been consumed."),
    RATE_LIMIT_175(ResponseStatus.WARNING,
            "You're sending more than 175 requests per second."),

    // Denied messages
    BLOCKED_BY_PROXY(ResponseStatus.DENIED,
            "Your access to the API has been blocked due to using a proxy server to perform your query. Please signup for an account to re-enable access by proxy."),
    FREE_QUERIES_EXHAUSTED_1000(ResponseStatus.DENIED,
            "1,000 Free queries exhausted. Please try the API again tomorrow or purchase a higher paid plan."),
    FREE_QUERIES_EXHAUSTED_1000_BURST(ResponseStatus.DENIED,
            "1,000 Free queries exhausted and a burst token has already been consumed."),
    FREE_QUERIES_EXHAUSTED_100(ResponseStatus.DENIED,
            "100 queries exhausted, if you sign up for a free API key you'll be able to make 1,000 free queries per day."),
    RATE_LIMIT_200(ResponseStatus.DENIED,
            "You're sending more than 200 requests per second."),

    // Error messages
    NO_VALID_IPS(ResponseStatus.ERROR,
            "No valid IP addresses supplied.");

    // Pre-computed lowercase lookup map for O(1) message matching in fromMessage()
    private static final Map<String, StatusMessage> BY_MESSAGE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(
                    sm -> sm.message.toLowerCase(),
                    sm -> sm));

    private final ResponseStatus status;
    private final String message;

    StatusMessage(ResponseStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public ResponseStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    /**
     * Resolves a raw API message string to its enum constant via case-insensitive
     * lookup. Returns {@code null} if the message is {@code null} or unrecognized.
     */
    public static StatusMessage fromMessage(String message) {
        if (message == null) {
            return null;
        }
        return BY_MESSAGE.get(message.trim().toLowerCase());
    }
}
