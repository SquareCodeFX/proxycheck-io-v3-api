package io.proxycheck.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Detection history for an IP address, indicating whether positive detections
 * have been or will be delisted from the service.
 *
 * <p>If {@code delisted} is {@code false}, the IP is still listed and the date-time
 * indicates when delisting is planned. If {@code delisted} is {@code true}, the
 * date-time indicates when the IP was last delisted.
 */
public record DetectionHistory(
        Boolean delisted,
        @SerializedName("delist_datetime") String delistDatetime
) {}
