package io.proxycheck.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Fluent builder for proxycheck.io API query parameters. Each method maps to a
 * documented API flag and returns {@code this} for chaining.
 *
 * <p>Use one of the factory presets ({@link #detailed()}, {@link #minimal()},
 * {@link #withNode()}) for common configurations, or start from scratch with
 * {@link #create()} to mix and match individual flags.
 *
 * <p>Parameters are accumulated in insertion order and serialized into a URL query
 * string via {@link #toQueryString()}.
 */
public final class QueryFlags {

    // LinkedHashMap preserves insertion order for deterministic query strings
    private final Map<String, String> params = new LinkedHashMap<>();
    private String cachedQueryString;

    private QueryFlags() {}

    /**
     * Creates a new empty {@code QueryFlags} instance with no parameters set.
     */
    public static QueryFlags create() {
        return new QueryFlags();
    }

    /**
     * Preset for verbose responses: enables cluster node info and limits
     * detections to the last 7 days.
     */
    public static QueryFlags detailed() {
        return create().node(true).days(7);
    }

    /**
     * Preset for minimal responses: enables the short (flat) response format.
     */
    public static QueryFlags minimal() {
        return create().shortResponse(true);
    }

    /**
     * Preset that only enables the cluster node identifier in the response.
     */
    public static QueryFlags withNode() {
        return create().node(true);
    }

    /**
     * Includes or excludes the cluster node identifier in the API response.
     *
     * @param enabled {@code true} to include the node field
     */
    public QueryFlags node(boolean enabled) {
        cachedQueryString = null;
        params.put("node", enabled ? "1" : "0");
        return this;
    }

    /**
     * Toggles the short (flat) response format. When enabled, the API returns a
     * simplified JSON structure with the address inlined rather than nested.
     *
     * @param enabled {@code true} to request the short response format
     */
    public QueryFlags shortResponse(boolean enabled) {
        cachedQueryString = null;
        params.put("short", enabled ? "1" : "0");
        return this;
    }

    /**
     * Toggles pretty-printed JSON output from the API.
     *
     * @param enabled {@code true} to request indented JSON
     */
    public QueryFlags prettyPrint(boolean enabled) {
        cachedQueryString = null;
        params.put("p", enabled ? "1" : "0");
        return this;
    }

    /**
     * Limits detection history to the specified number of days.
     *
     * @param days number of days to look back (must be &ge; 1)
     * @throws IllegalArgumentException if {@code days} is less than 1
     */
    public QueryFlags days(int days) {
        if (days < 1) {
            throw new IllegalArgumentException("days must be >= 1");
        }
        cachedQueryString = null;
        params.put("days", String.valueOf(days));
        return this;
    }

    /**
     * Attaches a custom tag to the query for tracking and analytics in the
     * proxycheck.io dashboard.
     *
     * @param tag the tag label to associate with this query
     */
    public QueryFlags tag(String tag) {
        cachedQueryString = null;
        params.put("tag", tag);
        return this;
    }

    /**
     * Disables query tagging by sending {@code tag=0}.
     */
    public QueryFlags noTag() {
        cachedQueryString = null;
        params.put("tag", "0");
        return this;
    }

    /**
     * Pins the request to a specific API version string (e.g., "11-February-2026").
     *
     * @param version the API version identifier
     */
    public QueryFlags ver(String version) {
        cachedQueryString = null;
        params.put("ver", version);
        return this;
    }

    /**
     * Returns {@code true} if the short response format is enabled.
     */
    boolean isShort() {
        return "1".equals(params.get("short"));
    }

    /**
     * Serializes all accumulated parameters into a URL query string fragment
     * (e.g., {@code "&node=1&days=7"}). Returns an empty string if no
     * parameters have been set.
     */
    String toQueryString() {
        if (cachedQueryString != null) {
            return cachedQueryString;
        }
        if (params.isEmpty()) {
            return cachedQueryString = "";
        }
        var joiner = new StringJoiner("&", "&", "");
        params.forEach((key, value) ->
                joiner.add(key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)));
        return cachedQueryString = joiner.toString();
    }
}
