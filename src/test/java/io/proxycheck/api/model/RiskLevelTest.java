package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RiskLevelTest {

    @Test
    void fromScoreLow() {
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(0));
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(10));
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(25));
    }

    @Test
    void fromScoreMedium() {
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(26));
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(40));
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(50));
    }

    @Test
    void fromScoreHigh() {
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(51));
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(60));
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(75));
    }

    @Test
    void fromScoreVeryHigh() {
        assertEquals(RiskLevel.VERY_HIGH, RiskLevel.fromScore(76));
        assertEquals(RiskLevel.VERY_HIGH, RiskLevel.fromScore(90));
        assertEquals(RiskLevel.VERY_HIGH, RiskLevel.fromScore(100));
    }

    @Test
    void fromScoreInvalid() {
        assertThrows(IllegalArgumentException.class, () -> RiskLevel.fromScore(-1));
        assertThrows(IllegalArgumentException.class, () -> RiskLevel.fromScore(101));
    }

    @Test
    void ranges() {
        assertEquals(0, RiskLevel.LOW.minScore());
        assertEquals(25, RiskLevel.LOW.maxScore());
        assertEquals(76, RiskLevel.VERY_HIGH.minScore());
        assertEquals(100, RiskLevel.VERY_HIGH.maxScore());
    }
}
