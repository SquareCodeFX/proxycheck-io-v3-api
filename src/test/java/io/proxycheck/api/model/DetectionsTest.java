package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DetectionsTest {

    @Test
    void riskLevelFromDetections() {
        var detections = new Detections(false, false, false, false, false, true, false, 33, 100, null, null);
        assertEquals(RiskLevel.MEDIUM, detections.riskLevel());
    }

    @Test
    void accessRecommendationNotAnonymous() {
        var detections = new Detections(false, false, false, false, false, true, false, 10, 100, null, null);
        assertEquals(AccessRecommendation.ALLOW, detections.accessRecommendation());
    }

    @Test
    void accessRecommendationAnonymous() {
        var detections = new Detections(false, true, false, false, false, false, true, 10, 100, null, null);
        assertEquals(AccessRecommendation.CHALLENGE, detections.accessRecommendation());
    }

    @Test
    void accessRecommendationHighRiskAnonymous() {
        var detections = new Detections(true, false, false, false, false, false, true, 80, 100, null, null);
        assertEquals(AccessRecommendation.DENY, detections.accessRecommendation());
    }

    @Test
    void nullRiskReturnsNull() {
        var detections = new Detections(false, false, false, false, false, false, false, null, null, null, null);
        assertNull(detections.riskLevel());
        assertNull(detections.accessRecommendation());
    }

    @Test
    void nullSafeConvenienceMethods() {
        var detections = new Detections(null, null, null, null, null, null, null, null, null, null, null);
        assertFalse(detections.isProxy());
        assertFalse(detections.isVpn());
        assertFalse(detections.isCompromised());
        assertFalse(detections.isScraper());
        assertFalse(detections.isTor());
        assertFalse(detections.isHosting());
        assertFalse(detections.isAnonymous());
        assertFalse(detections.isThreat());
    }

    @Test
    void isThreatDetectsAnyThreatFlag() {
        assertFalse(new Detections(false, false, false, false, false, true, false, 10, 100, null, null).isThreat());
        assertTrue(new Detections(true, false, false, false, false, false, false, 50, 100, null, null).isThreat());
        assertTrue(new Detections(false, true, false, false, false, false, false, 50, 100, null, null).isThreat());
        assertTrue(new Detections(false, false, true, false, false, false, false, 50, 100, null, null).isThreat());
        assertTrue(new Detections(false, false, false, true, false, false, false, 50, 100, null, null).isThreat());
        assertTrue(new Detections(false, false, false, false, true, false, false, 50, 100, null, null).isThreat());
    }

    @Test
    void convenienceMethodsMatchRecordAccessors() {
        var detections = new Detections(true, false, true, false, true, true, false, 75, 90, "2025-01-01", "2025-03-01");
        assertTrue(detections.isProxy());
        assertFalse(detections.isVpn());
        assertTrue(detections.isCompromised());
        assertFalse(detections.isScraper());
        assertTrue(detections.isTor());
        assertTrue(detections.isHosting());
        assertFalse(detections.isAnonymous());
        assertTrue(detections.isThreat());
    }

    @Test
    void compactConstructorRejectsInvalidRisk() {
        assertThrows(IllegalArgumentException.class, () ->
                new Detections(null, null, null, null, null, null, null, -1, null, null, null));
        assertThrows(IllegalArgumentException.class, () ->
                new Detections(null, null, null, null, null, null, null, 101, null, null, null));
    }

    @Test
    void compactConstructorRejectsInvalidConfidence() {
        assertThrows(IllegalArgumentException.class, () ->
                new Detections(null, null, null, null, null, null, null, null, -1, null, null));
        assertThrows(IllegalArgumentException.class, () ->
                new Detections(null, null, null, null, null, null, null, null, 101, null, null));
    }

    @Test
    void compactConstructorAcceptsValidBounds() {
        assertDoesNotThrow(() -> new Detections(null, null, null, null, null, null, null, 0, 0, null, null));
        assertDoesNotThrow(() -> new Detections(null, null, null, null, null, null, null, 100, 100, null, null));
    }
}
