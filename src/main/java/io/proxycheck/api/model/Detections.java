package io.proxycheck.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Threat detection results for an IP address. Contains boolean flags for each
 * detection type, a risk score (0-100), a confidence score (0-100), and
 * first/last seen timestamps.
 *
 * <p>Multiple detection types can be {@code true} simultaneously (e.g., an IP
 * can be both a VPN and anonymous). Use {@link #isThreat()} for an aggregate
 * check, or inspect individual flags for finer control.
 *
 * <p>All boolean fields use {@link Boolean} (boxed) because the API may omit
 * them; the {@code isXxx()} accessors treat {@code null} as {@code false}.
 */
public record Detections(
        Boolean proxy,
        Boolean vpn,
        Boolean compromised,
        Boolean scraper,
        Boolean tor,
        Boolean hosting,
        Boolean anonymous,
        Integer risk,
        Integer confidence,
        @SerializedName("first_seen") String firstSeen,
        @SerializedName("last_seen") String lastSeen
) {

    // Compact constructor validates score ranges to catch malformed API data early
    public Detections {
        if (risk != null && (risk < 0 || risk > 100)) {
            throw new IllegalArgumentException("risk must be 0-100, got: " + risk);
        }
        if (confidence != null && (confidence < 0 || confidence > 100)) {
            throw new IllegalArgumentException("confidence must be 0-100, got: " + confidence);
        }
    }

    // Boolean.TRUE.equals() is used throughout to treat null as false (API may omit fields)
    public boolean isProxy() {
        return Boolean.TRUE.equals(proxy);
    }

    public boolean isVpn() {
        return Boolean.TRUE.equals(vpn);
    }

    public boolean isCompromised() {
        return Boolean.TRUE.equals(compromised);
    }

    public boolean isScraper() {
        return Boolean.TRUE.equals(scraper);
    }

    public boolean isTor() {
        return Boolean.TRUE.equals(tor);
    }

    public boolean isHosting() {
        return Boolean.TRUE.equals(hosting);
    }

    public boolean isAnonymous() {
        return Boolean.TRUE.equals(anonymous);
    }

    // Hosting alone is not considered a threat (many legitimate services use hosting IPs)
    public boolean isThreat() {
        return isProxy() || isVpn() || isCompromised() || isScraper() || isTor();
    }

    /** Maps the numeric risk score to a {@link RiskLevel} tier, or {@code null} if absent. */
    public RiskLevel riskLevel() {
        return risk != null ? RiskLevel.fromScore(risk) : null;
    }

    /**
     * Computes an {@link AccessRecommendation} from the risk score and anonymity
     * flag. Returns {@code null} if either value is unavailable.
     */
    public AccessRecommendation accessRecommendation() {
        return (risk != null && anonymous != null) ? AccessRecommendation.evaluate(risk, anonymous) : null;
    }
}
