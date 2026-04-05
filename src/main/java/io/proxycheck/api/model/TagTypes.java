package io.proxycheck.api.model;

/**
 * Detection type counts within a tag entry.
 * Represents how many detections of each classification were recorded
 * for a given tag across the selected time window.
 *
 * @param total     total number of positive detections for this tag
 * @param proxy     number of proxy detections
 * @param vpn       number of VPN detections
 * @param rule      number of custom rule detections
 * @param blacklist number of blacklist detections
 */
public record TagTypes(
        int total,
        Integer proxy,
        Integer vpn,
        Integer rule,
        Integer blacklist
) {}
