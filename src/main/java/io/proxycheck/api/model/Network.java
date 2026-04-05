package io.proxycheck.api.model;

/**
 * Network metadata for an IP address as returned by the API.
 *
 * @param asn          the Autonomous System Number (e.g., "AS13335")
 * @param range        the IP range/CIDR block this address belongs to
 * @param hostname     reverse DNS hostname, or {@code null} if unavailable
 * @param provider     the ISP or hosting provider name
 * @param organisation the registered organization for this network
 * @param type         the raw network allocation type string (e.g., "Hosting", "Residential")
 */
public record Network(
        String asn,
        String range,
        String hostname,
        String provider,
        String organisation,
        String type
) {

    /**
     * Returns the network type as an enum, or {@code null} if unknown.
     */
    public NetworkType networkType() {
        return NetworkType.fromApiValue(type);
    }
}
