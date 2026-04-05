package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessRecommendationTest {

    @Test
    void lowRiskNotAnonymous() {
        assertEquals(AccessRecommendation.ALLOW, AccessRecommendation.evaluate(0, false));
        assertEquals(AccessRecommendation.ALLOW, AccessRecommendation.evaluate(25, false));
    }

    @Test
    void lowRiskAnonymous() {
        assertEquals(AccessRecommendation.CHALLENGE, AccessRecommendation.evaluate(0, true));
        assertEquals(AccessRecommendation.CHALLENGE, AccessRecommendation.evaluate(25, true));
    }

    @Test
    void mediumRisk() {
        assertEquals(AccessRecommendation.CHALLENGE, AccessRecommendation.evaluate(30, false));
        assertEquals(AccessRecommendation.CHALLENGE, AccessRecommendation.evaluate(30, true));
        assertEquals(AccessRecommendation.CHALLENGE, AccessRecommendation.evaluate(50, false));
        assertEquals(AccessRecommendation.CHALLENGE, AccessRecommendation.evaluate(50, true));
    }

    @Test
    void highRiskNotAnonymous() {
        assertEquals(AccessRecommendation.CHALLENGE, AccessRecommendation.evaluate(60, false));
        assertEquals(AccessRecommendation.CHALLENGE, AccessRecommendation.evaluate(75, false));
    }

    @Test
    void highRiskAnonymous() {
        assertEquals(AccessRecommendation.DENY, AccessRecommendation.evaluate(60, true));
        assertEquals(AccessRecommendation.DENY, AccessRecommendation.evaluate(75, true));
    }

    @Test
    void veryHighRisk() {
        assertEquals(AccessRecommendation.DENY, AccessRecommendation.evaluate(76, false));
        assertEquals(AccessRecommendation.DENY, AccessRecommendation.evaluate(100, true));
        assertEquals(AccessRecommendation.DENY, AccessRecommendation.evaluate(100, false));
    }
}
