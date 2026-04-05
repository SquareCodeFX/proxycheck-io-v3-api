package io.proxycheck.api;

import io.proxycheck.api.exception.ProxyCheckException;
import io.proxycheck.api.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * HTTP client for the proxycheck.io Dashboard API. Provides typed access to
 * all dashboard endpoints: detection export, tag export, account usage, query
 * volume, custom list management, custom rule control, and CORS manipulation.
 *
 * <p>All dashboard API calls are free and do not count against the regular
 * proxy/VPN query quota. A dashboard API key and the Dashboard API toggle
 * must be enabled in the proxycheck.io account settings.
 *
 * <p>Obtain an instance via {@link #builder()} for standalone use or via
 * {@link ProxyCheckClient#dashboard()} to share the underlying HTTP connection
 * and retry configuration with an existing client.
 *
 * <pre>{@code
 * // Standalone
 * try (var dash = DashboardClient.builder().apiKey("your-key").build()) {
 *     AccountUsage usage = dash.getUsage();
 *     System.out.println("Queries today: " + usage.queriesToday());
 * }
 *
 * // Shared with ProxyCheckClient
 * try (var client = ProxyCheckClient.of("your-key")) {
 *     DashboardClient dash = client.dashboard();
 *     List<Detection> detections = dash.exportDetections();
 * }
 * }</pre>
 */
public final class DashboardClient implements AutoCloseable {

    private static final String BASE_URL = "https://proxycheck.io/dashboard/";
    private static final String USER_AGENT = "proxycheck-java/1.0.0";

    private final String apiKey;
    private final HttpClient httpClient;
    private final Duration timeout;
    private final RetryPolicy retryPolicy;
    /** True when this client created the HttpClient and is responsible for closing it. */
    private final boolean ownsHttpClient;

    private DashboardClient(String apiKey, HttpClient httpClient, Duration timeout,
                            RetryPolicy retryPolicy, boolean ownsHttpClient) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.timeout = timeout;
        this.retryPolicy = retryPolicy;
        this.ownsHttpClient = ownsHttpClient;
    }

    /**
     * Creates a {@link DashboardClient} that shares the HTTP connection pool and
     * retry policy of the given {@link ProxyCheckClient}. The dashboard client does
     * not own the underlying {@link HttpClient}; closing it has no effect.
     */
    static DashboardClient from(String apiKey, HttpClient httpClient, Duration timeout,
                                RetryPolicy retryPolicy) {
        return new DashboardClient(apiKey, httpClient, timeout, retryPolicy, false);
    }

    /** Returns a new {@link Builder} for standalone configuration. */
    public static Builder builder() {
        return new Builder();
    }

    // =========================================================================
    // Detection export
    // =========================================================================

    /**
     * Exports the 100 most recent positive detections (proxy/VPN) from the dashboard.
     *
     * @return ordered list of detection entries, most recent first
     */
    public List<Detection> exportDetections() {
        return exportDetections(100, 0, null);
    }

    /**
     * Exports recent positive detections with pagination support.
     *
     * @param limit  maximum number of entries to return (max 100)
     * @param offset number of entries to skip for pagination
     * @return ordered list of detection entries
     */
    public List<Detection> exportDetections(int limit, int offset) {
        return exportDetections(limit, offset, null);
    }

    /**
     * Exports recent positive detections with pagination and an optional text filter.
     *
     * <p>This endpoint has a per-second request limit of 2 with a 10-second resolution
     * (20 requests per 10 seconds). Exceeding the limit delays the request rather than
     * returning an error.
     *
     * @param limit  maximum number of entries to return (max 100)
     * @param offset number of entries to skip for pagination
     * @param filter optional search term to filter results by IP address or tag;
     *               {@code null} for no filtering
     * @return ordered list of detection entries
     */
    public List<Detection> exportDetections(int limit, int offset, String filter) {
        var url = new StringBuilder(baseUrl("export/detections/"))
                .append("&json=1")
                .append("&limit=").append(limit)
                .append("&offset=").append(offset);
        if (filter != null && !filter.isBlank()) {
            url.append("&filter=").append(encode(filter));
        }
        return DashboardParser.parseDetections(get(url.toString()));
    }

    // =========================================================================
    // Tag export
    // =========================================================================

    /**
     * Exports the 100 most frequently used tags from today.
     *
     * @return map of tag name → {@link TagEntry}
     */
    public Map<String, TagEntry> exportTags() {
        return exportTags(100, 0, false, 1);
    }

    /**
     * Exports tags for the past {@code days} days with full control over pagination
     * and whether associated IP addresses should be included.
     *
     * @param limit            maximum number of tags to return (up to 1000)
     * @param offset           number of tags to skip for pagination
     * @param includeAddresses when {@code true}, each tag entry includes the IP
     *                         addresses and per-address check counts
     * @param days             number of days in the past to include (e.g. 7 = last 7 days)
     * @return map of tag name → {@link TagEntry}
     */
    public Map<String, TagEntry> exportTags(int limit, int offset, boolean includeAddresses, int days) {
        var url = new StringBuilder(baseUrl("export/tags/"))
                .append("&limit=").append(limit)
                .append("&offset=").append(offset)
                .append("&addresses=").append(includeAddresses ? 1 : 0)
                .append("&days=").append(days);
        return DashboardParser.parseTags(get(url.toString()));
    }

    /**
     * Exports tags within an explicit time range specified as Unix timestamps.
     *
     * <p>Note: {@code start} is the more recent boundary and {@code end} is the
     * older boundary — the API looks backwards in time, so start &gt; end.
     *
     * @param limit            maximum number of tags to return (up to 1000)
     * @param offset           number of tags to skip for pagination
     * @param includeAddresses when {@code true}, each tag entry includes IP addresses
     * @param start            Unix timestamp for the recent boundary of the range
     * @param end              Unix timestamp for the distant (older) boundary of the range
     * @return map of tag name → {@link TagEntry}
     */
    public Map<String, TagEntry> exportTags(int limit, int offset, boolean includeAddresses,
                                            long start, long end) {
        var url = new StringBuilder(baseUrl("export/tags/"))
                .append("&limit=").append(limit)
                .append("&offset=").append(offset)
                .append("&addresses=").append(includeAddresses ? 1 : 0)
                .append("&start=").append(start)
                .append("&end=").append(end);
        return DashboardParser.parseTags(get(url.toString()));
    }

    // =========================================================================
    // Account usage
    // =========================================================================

    /**
     * Returns today's account usage statistics (query counts, daily limit, plan tier).
     * Stats are delayed by a few minutes on the server side.
     *
     * @return current account usage
     */
    public AccountUsage getUsage() {
        return DashboardParser.parseAccountUsage(get(baseUrl("export/usage/")));
    }

    /**
     * Returns the past 30 days of daily query volume, broken down by detection type.
     *
     * @return map of date string ({@code YYYY-MM-DD}) → {@link QueryVolume}
     */
    public Map<String, QueryVolume> getQueryVolume() {
        return DashboardParser.parseQueryVolume(get(baseUrl("export/queries/") + "&json=1"));
    }

    // =========================================================================
    // Custom lists – read
    // =========================================================================

    /**
     * Returns the names of all custom lists configured on the account.
     *
     * @return list of custom list names
     */
    public List<String> listNames() {
        return DashboardParser.parseListNames(get(baseUrl("lists/print/")));
    }

    /**
     * Returns the entries of a specific custom list.
     *
     * @param listName the name of the list to print
     * @return ordered list of entries (IP addresses, CIDRs, etc.)
     */
    public List<String> printList(String listName) {
        Objects.requireNonNull(listName, "listName must not be null");
        return DashboardParser.parseListEntries(get(baseUrl("lists/print/" + encode(listName))));
    }

    /**
     * Returns the entries of a specific custom list, also confirming the list's type.
     * Passing the known type matches the documented example URL which includes
     * {@code &type=whitelist}.
     *
     * @param listName the name of the list to print
     * @param type     the expected type of this list (whitelist, blacklist, or none)
     * @return ordered list of entries (IP addresses, CIDRs, etc.)
     */
    public List<String> printList(String listName, ListType type) {
        Objects.requireNonNull(listName, "listName must not be null");
        Objects.requireNonNull(type, "type must not be null");
        String url = baseUrl("lists/print/" + encode(listName)) + "&type=" + type.value();
        return DashboardParser.parseListEntries(get(url));
    }

    // =========================================================================
    // Custom lists – mutations
    // =========================================================================

    /**
     * Appends entries to an existing custom list.
     * Entries are sent as the POST {@code data} field; use {@code \r\n} as a
     * line separator within the string to add multiple entries in one call.
     *
     * @param listName the target list name
     * @param type     the list classification (whitelist, blacklist, or none)
     * @param entries  newline-separated entries to append
     * @return the API response indicating success or failure
     */
    public DashboardResponse addToList(String listName, ListType type, String entries) {
        Objects.requireNonNull(listName, "listName must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(entries, "entries must not be null");
        return DashboardParser.parseDashboardResponse(
                post(listActionUrl("add", listName, type), "data=" + encode(entries)));
    }

    /**
     * Appends a collection of entries to an existing custom list.
     * Entries are joined with {@code \r\n} before being sent.
     *
     * @param listName the target list name
     * @param type     the list classification
     * @param entries  entries to add (each becomes a separate list entry)
     * @return the API response indicating success or failure
     */
    public DashboardResponse addToList(String listName, ListType type, Collection<String> entries) {
        return addToList(listName, type, joinEntries(entries));
    }

    /**
     * Removes entries from a custom list.
     *
     * @param listName the target list name
     * @param type     the list classification
     * @param entries  newline-separated entries to remove
     * @return the API response indicating success or failure
     */
    public DashboardResponse removeFromList(String listName, ListType type, String entries) {
        Objects.requireNonNull(listName, "listName must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(entries, "entries must not be null");
        return DashboardParser.parseDashboardResponse(
                post(listActionUrl("remove", listName, type), "data=" + encode(entries)));
    }

    /**
     * Removes a collection of entries from a custom list.
     *
     * @param listName the target list name
     * @param type     the list classification
     * @param entries  entries to remove
     * @return the API response indicating success or failure
     */
    public DashboardResponse removeFromList(String listName, ListType type, Collection<String> entries) {
        return removeFromList(listName, type, joinEntries(entries));
    }

    /**
     * Replaces the entire contents of a custom list with the provided entries.
     * All existing entries are discarded.
     *
     * @param listName the target list name
     * @param type     the list classification
     * @param entries  newline-separated entries that will become the new list contents
     * @return the API response indicating success or failure
     */
    public DashboardResponse setList(String listName, ListType type, String entries) {
        Objects.requireNonNull(listName, "listName must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(entries, "entries must not be null");
        return DashboardParser.parseDashboardResponse(
                post(listActionUrl("set", listName, type), "data=" + encode(entries)));
    }

    /**
     * Replaces the entire contents of a custom list with the provided entries.
     *
     * @param listName the target list name
     * @param type     the list classification
     * @param entries  entries that will replace all existing list contents
     * @return the API response indicating success or failure
     */
    public DashboardResponse setList(String listName, ListType type, Collection<String> entries) {
        return setList(listName, type, joinEntries(entries));
    }

    /**
     * Clears all entries from a custom list without deleting the list itself.
     *
     * @param listName the target list name
     * @return the API response indicating success or failure
     */
    public DashboardResponse clearList(String listName) {
        Objects.requireNonNull(listName, "listName must not be null");
        return DashboardParser.parseDashboardResponse(
                get(baseUrl("lists/clear/" + encode(listName))));
    }

    /**
     * Permanently erases a custom list and all of its contents.
     *
     * @param listName the list to erase
     * @return the API response indicating success or failure
     */
    public DashboardResponse eraseList(String listName) {
        Objects.requireNonNull(listName, "listName must not be null");
        return DashboardParser.parseDashboardResponse(
                get(baseUrl("lists/erase/" + encode(listName))));
    }

    /**
     * Schedules a force-download for a list that pulls its content from an external URL.
     * The download will occur within the next 60 seconds.
     *
     * @param listName the list to force-download
     * @return the API response indicating success or failure
     */
    public DashboardResponse forceDownloadList(String listName) {
        Objects.requireNonNull(listName, "listName must not be null");
        return DashboardParser.parseDashboardResponse(
                get(baseUrl("lists/forcedl/" + encode(listName))));
    }

    // =========================================================================
    // Custom rules
    // =========================================================================

    /**
     * Returns a map of all custom rules (rule ID → rule name).
     *
     * @return map of rule identifier to rule name
     */
    public Map<String, String> printRules() {
        return DashboardParser.parseRules(get(baseUrl("rules/print/")));
    }

    /**
     * Returns details for a specific rule identified by its ID in the URL path.
     *
     * @param ruleId the rule identifier (e.g. {@code "4o4h6107e089"})
     * @return map containing the rule's data fields
     */
    public Map<String, String> printRule(String ruleId) {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        return DashboardParser.parseRules(get(baseUrl("rules/print/" + encode(ruleId))));
    }

    /**
     * Returns details for a specific rule identified by its name or ID supplied
     * as a POST field. Use this when referencing rules by their human-readable name.
     *
     * @param nameOrId the rule name or ID (e.g. {@code "Elevate Risk Score"})
     * @return map containing the rule's data fields
     */
    public Map<String, String> printRuleByName(String nameOrId) {
        Objects.requireNonNull(nameOrId, "nameOrId must not be null");
        return DashboardParser.parseRules(post(baseUrl("rules/print/"), "name=" + encode(nameOrId)));
    }

    /**
     * Enables a custom rule by its ID (supplied in the URL path).
     *
     * @param ruleId the rule identifier (e.g. {@code "4o4h6107e089"})
     * @return the API response indicating success or failure
     */
    public DashboardResponse enableRule(String ruleId) {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        return DashboardParser.parseDashboardResponse(
                get(baseUrl("rules/enable/" + encode(ruleId))));
    }

    /**
     * Disables a custom rule by its ID (supplied in the URL path).
     *
     * @param ruleId the rule identifier (e.g. {@code "4o4h6107e089"})
     * @return the API response indicating success or failure
     */
    public DashboardResponse disableRule(String ruleId) {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        return DashboardParser.parseDashboardResponse(
                get(baseUrl("rules/disable/" + encode(ruleId))));
    }

    /**
     * Enables a custom rule using its name or ID supplied as the POST {@code name} field.
     * Use this when targeting a rule by its human-readable name instead of its ID.
     * IDs are also accepted in the POST {@code name} field for compatibility.
     *
     * @param nameOrId the rule name or ID (e.g. {@code "Elevate Risk Score"})
     * @return the API response indicating success or failure
     */
    public DashboardResponse enableRuleByName(String nameOrId) {
        Objects.requireNonNull(nameOrId, "nameOrId must not be null");
        return DashboardParser.parseDashboardResponse(
                post(baseUrl("rules/enable/"), "name=" + encode(nameOrId)));
    }

    /**
     * Disables a custom rule using its name or ID supplied as the POST {@code name} field.
     *
     * @param nameOrId the rule name or ID (e.g. {@code "Elevate Risk Score"})
     * @return the API response indicating success or failure
     */
    public DashboardResponse disableRuleByName(String nameOrId) {
        Objects.requireNonNull(nameOrId, "nameOrId must not be null");
        return DashboardParser.parseDashboardResponse(
                post(baseUrl("rules/disable/"), "name=" + encode(nameOrId)));
    }

    // =========================================================================
    // CORS manipulation
    // =========================================================================

    /**
     * Returns the list of allowed CORS origin domains configured on the account.
     *
     * @return list of origin domain strings
     */
    public List<String> listOrigins() {
        return DashboardParser.parseCorsOrigins(get(baseUrl("cors/list/")));
    }

    /**
     * Appends one or more origins to the CORS allow-list.
     *
     * @param origins origins to add (e.g. {@code "https://example.com"})
     * @return the API response, including the updated {@link DashboardResponse#count()}
     */
    public DashboardResponse addOrigins(String... origins) {
        Objects.requireNonNull(origins, "origins must not be null");
        return addOrigins(List.of(origins));
    }

    /**
     * Appends a collection of origins to the CORS allow-list.
     * Multiple origins are joined with {@code \r\n} before being sent.
     *
     * @param origins origins to add
     * @return the API response, including the updated origin count
     */
    public DashboardResponse addOrigins(Collection<String> origins) {
        Objects.requireNonNull(origins, "origins must not be null");
        return DashboardParser.parseDashboardResponse(
                post(baseUrl("cors/add/"), "data=" + encode(joinEntries(origins))));
    }

    /**
     * Removes one or more origins from the CORS allow-list.
     *
     * @param origins origins to remove
     * @return the API response, including the updated origin count
     */
    public DashboardResponse removeOrigins(String... origins) {
        Objects.requireNonNull(origins, "origins must not be null");
        return removeOrigins(List.of(origins));
    }

    /**
     * Removes a collection of origins from the CORS allow-list.
     *
     * @param origins origins to remove
     * @return the API response, including the updated origin count
     */
    public DashboardResponse removeOrigins(Collection<String> origins) {
        Objects.requireNonNull(origins, "origins must not be null");
        return DashboardParser.parseDashboardResponse(
                post(baseUrl("cors/remove/"), "data=" + encode(joinEntries(origins))));
    }

    /**
     * Replaces the entire CORS allow-list with the provided origins.
     *
     * @param origins the new complete set of allowed origins
     * @return the API response, including the updated origin count
     */
    public DashboardResponse setOrigins(String... origins) {
        Objects.requireNonNull(origins, "origins must not be null");
        return setOrigins(List.of(origins));
    }

    /**
     * Replaces the entire CORS allow-list with the provided origins.
     *
     * @param origins the new complete set of allowed origins
     * @return the API response, including the updated origin count
     */
    public DashboardResponse setOrigins(Collection<String> origins) {
        Objects.requireNonNull(origins, "origins must not be null");
        return DashboardParser.parseDashboardResponse(
                post(baseUrl("cors/set/"), "data=" + encode(joinEntries(origins))));
    }

    /**
     * Clears all origins from the CORS allow-list.
     *
     * @return the API response indicating success or failure
     */
    public DashboardResponse clearOrigins() {
        return DashboardParser.parseDashboardResponse(get(baseUrl("cors/clear/")));
    }

    // =========================================================================
    // HTTP internals
    // =========================================================================

    private String get(String url) {
        return executeWithRetry(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build());
    }

    private String post(String url, String formData) {
        return executeWithRetry(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build());
    }

    private String executeWithRetry(HttpRequest request) {
        ProxyCheckException lastException = null;
        int maxAttempts = 1 + retryPolicy.maxRetries();
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                if (attempt > 0) {
                    Duration delay = retryPolicy.delayForAttempt(attempt - 1);
                    Thread.sleep(delay.toMillis());
                }
                return execute(request);
            } catch (ProxyCheckException e) {
                lastException = e;
                if (!retryPolicy.isRetryable(e) && !retryPolicy.isRetryable(e.getCause())) {
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

    private String execute(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 500) {
                throw new ProxyCheckException("Server error: HTTP " + response.statusCode(),
                        response.statusCode());
            }
            return response.body();
        } catch (IOException e) {
            throw new ProxyCheckException("HTTP request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProxyCheckException("HTTP request interrupted", e);
        }
    }

    // =========================================================================
    // URL helpers
    // =========================================================================

    /**
     * Returns the base URL for the given dashboard path with the API key already
     * appended as the first query parameter. Additional parameters should be
     * appended as {@code "&param=value"}.
     */
    private String baseUrl(String path) {
        return BASE_URL + path + "?key=" + encode(apiKey);
    }

    /** Builds the URL for a list mutation action (add/remove/set), including the type flag. */
    private String listActionUrl(String action, String listName, ListType type) {
        return baseUrl("lists/" + action + "/" + encode(listName)) + "&type=" + type.value();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Joins a collection of entries with {@code \r\n} as required by the API for
     * multi-entry POST data fields.
     */
    private static String joinEntries(Collection<String> entries) {
        var joiner = new StringJoiner("\r\n");
        for (String e : entries) joiner.add(e);
        return joiner.toString();
    }

    @Override
    public void close() {
        if (ownsHttpClient) {
            httpClient.close();
        }
    }

    // =========================================================================
    // Builder
    // =========================================================================

    /**
     * Fluent builder for configuring a standalone {@link DashboardClient}.
     *
     * <p>Defaults:
     * <ul>
     *   <li>Timeout: 30 seconds</li>
     *   <li>Retry: 3 retries with exponential backoff (500ms initial, 2x multiplier, 8s cap)</li>
     * </ul>
     */
    public static final class Builder {
        private String apiKey;
        private Duration timeout = Duration.ofSeconds(30);
        private HttpClient httpClient;
        private RetryPolicy retryPolicy = RetryPolicy.defaultPolicy();

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
         * Provides a custom {@link HttpClient} to use. When set, this client is not
         * closed when the {@link DashboardClient} is closed.
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
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
         * Builds and returns a configured {@link DashboardClient}.
         *
         * @throws NullPointerException if the API key has not been set
         */
        public DashboardClient build() {
            Objects.requireNonNull(apiKey, "apiKey must be set");
            boolean ownsHttpClient = this.httpClient == null;
            HttpClient client = ownsHttpClient
                    ? HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .connectTimeout(timeout)
                        .build()
                    : this.httpClient;
            return new DashboardClient(apiKey, client, timeout, retryPolicy, ownsHttpClient);
        }
    }
}
