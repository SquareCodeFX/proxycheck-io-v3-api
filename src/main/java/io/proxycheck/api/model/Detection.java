package io.proxycheck.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * A single positive detection entry exported from the dashboard.
 * Positive detections are queries that yielded a proxy or VPN result.
 *
 * @param timeFormatted human-readable timestamp (e.g. "23rd of November 2017 at 7:01 am")
 * @param timeRaw       Unix timestamp as a string
 * @param address       the detected IP address
 * @param detectionType the type of detection (e.g. "vpn", "proxy")
 * @param answeringNode the cluster node that answered the query (e.g. "ATLAS")
 * @param tag           optional custom tag attached to the original query
 */
public record Detection(
        @SerializedName("time formatted") String timeFormatted,
        @SerializedName("time raw") String timeRaw,
        String address,
        @SerializedName("detection type") String detectionType,
        @SerializedName("answering node") String answeringNode,
        String tag
) {}
