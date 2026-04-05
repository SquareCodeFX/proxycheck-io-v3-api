package io.proxycheck.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Complete result for a single IP address check. Aggregates all sections the
 * API may return: network metadata, geolocation, device estimates, threat
 * detections, detection and attack history, and operator information.
 *
 * <p>Convenience accessors ({@link #isThreat()}, {@link #riskLevel()},
 * {@link #countryCode()}, etc.) safely delegate to nested objects and return
 * {@code null} when the underlying section is absent.
 *
 * @param address          the queried IP address
 * @param network          ASN, provider, hostname, and network type information
 * @param location         geolocation data (continent, country, city, coordinates)
 * @param deviceEstimate   estimated device count behind the IP and its subnet
 * @param detections       threat flags, risk/confidence scores, and timestamps
 * @param detectionHistory listing status and scheduled or past delist date
 * @param attackHistory    map of attack types to occurrence counts
 * @param operator         operator/VPN provider details and policies
 * @param lastUpdated      ISO 8601 timestamp of the last data update for this IP
 */
public record IpResult(
    String address,
    Network network,
    Location location,
    @SerializedName("device_estimate") DeviceEstimate deviceEstimate,
    Detections detections,
    @SerializedName("detection_history") DetectionHistory detectionHistory,
    @SerializedName("attack_history") AttackHistory attackHistory,
    Operator operator,
    @SerializedName("last_updated") String lastUpdated
) implements Result {

    /** Derives the risk level from the detection score, or {@code null} if unavailable. */
    public RiskLevel riskLevel() {
        return detections != null ? detections.riskLevel() : null;
    }

    /** Computes the access recommendation based on risk and anonymity, or {@code null}. */
    public AccessRecommendation accessRecommendation() {
        return detections != null ? detections.accessRecommendation() : null;
    }

    /** Returns {@code true} if this IP is flagged as a proxy, VPN, TOR, compromised, or scraper. */
    public boolean isThreat() {
        return detections != null && detections.isThreat();
    }

    /** Shortcut for {@code location().countryCode()}, or {@code null} if location is absent. */
    public String countryCode() {
        return location != null ? location.countryCode() : null;
    }

    /** Shortcut for {@code network().provider()}, or {@code null} if network is absent. */
    public String provider() {
        return network != null ? network.provider() : null;
    }

    /** Returns the network allocation type as an enum, or {@code null} if unknown. */
    public NetworkType networkType() {
        return network != null ? network.networkType() : null;
    }
}
