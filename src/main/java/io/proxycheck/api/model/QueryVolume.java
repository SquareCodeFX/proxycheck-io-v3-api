package io.proxycheck.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Daily query volume breakdown for a single day, as returned by the dashboard
 * query-volume export endpoint. Covers the past 30 days by default.
 *
 * @param totalQueries    total number of queries made on this day
 * @param positiveQueries queries that returned a positive (proxy/VPN) result
 * @param negativeQueries queries that returned a clean result
 * @param refusedQueries  queries refused due to quota or policy
 * @param proxy           number of proxy detections
 * @param vpn             number of VPN detections
 * @param compromised     number of compromised IP detections
 * @param scraper         number of scraper detections
 * @param tor             number of Tor exit node detections
 * @param hosting         number of hosting/datacenter IP detections
 * @param disposableEmail number of disposable email detections
 * @param reusableEmail   number of reusable (non-disposable) email checks
 * @param customRule      number of custom rule matches
 * @param blacklisted     number of blacklisted IP detections
 */
public record QueryVolume(
        @SerializedName("total_queries") int totalQueries,
        @SerializedName("positive_queries") int positiveQueries,
        @SerializedName("negative_queries") int negativeQueries,
        @SerializedName("refused_queries") int refusedQueries,
        int proxy,
        int vpn,
        int compromised,
        int scraper,
        int tor,
        int hosting,
        @SerializedName("disposable_email") int disposableEmail,
        @SerializedName("reusable_email") int reusableEmail,
        @SerializedName("custom_rule") int customRule,
        int blacklisted
) {}
