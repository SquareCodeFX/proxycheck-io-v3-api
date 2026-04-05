package io.proxycheck.api.model;

/**
 * Estimated number of unique devices seen behind an IP address and its subnet.
 * These values are approximations based on the API's traffic analysis.
 *
 * @param address estimated device count behind the specific IP address
 * @param subnet  estimated device count across the entire subnet
 */
public record DeviceEstimate(
        Integer address,
        Integer subnet
) {}
