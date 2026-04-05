package io.proxycheck.api.model;

/**
 * Risk classification tiers derived from the API's numeric risk score (0-100).
 * Each level maps to a non-overlapping score range used for threshold-based
 * access decisions. See {@link AccessRecommendation} for the recommended
 * allow/challenge/deny logic.
 */
public enum RiskLevel {

    /** Score 0-25: minimal risk, typically safe traffic. */
    LOW(0, 25),
    /** Score 26-50: moderate risk, warrants monitoring. */
    MEDIUM(26, 50),
    /** Score 51-75: elevated risk, likely suspicious activity. */
    HIGH(51, 75),
    /** Score 76-100: severe risk, strongly associated with abuse. */
    VERY_HIGH(76, 100);

    private final int minScore;
    private final int maxScore;

    RiskLevel(int minScore, int maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public int minScore() {
        return minScore;
    }

    public int maxScore() {
        return maxScore;
    }

    /**
     * Resolves a numeric risk score to its corresponding tier.
     *
     * @param score the risk score (0-100)
     * @return the matching {@code RiskLevel}
     * @throws IllegalArgumentException if the score is outside the 0-100 range
     */
    public static RiskLevel fromScore(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Risk score must be between 0 and 100, got: " + score);
        }
        if (score <= 25) return LOW;
        if (score <= 50) return MEDIUM;
        if (score <= 75) return HIGH;
        return VERY_HIGH;
    }
}
