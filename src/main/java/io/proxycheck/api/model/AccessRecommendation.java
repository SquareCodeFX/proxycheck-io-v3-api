package io.proxycheck.api.model;

/**
 * Recommended access decision based on risk score and anonymity status.
 * Use {@link #evaluate(int, boolean)} to compute the recommendation.
 */
public enum AccessRecommendation {

    ALLOW,
    CHALLENGE,
    DENY;

    // Anonymity raises the recommendation by one level (e.g., LOW -> CHALLENGE instead of ALLOW)
    public static AccessRecommendation evaluate(int riskScore, boolean anonymous) {
        RiskLevel level = RiskLevel.fromScore(riskScore);
        return switch (level) {
            case LOW -> anonymous ? CHALLENGE : ALLOW;
            case MEDIUM -> CHALLENGE;
            case HIGH -> anonymous ? DENY : CHALLENGE;
            case VERY_HIGH -> DENY;
        };
    }
}
