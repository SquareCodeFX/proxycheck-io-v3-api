package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpResultTest {

    @Test
    void convenienceAccessorsWithFullData() {
        var network = new Network("AS16509", "98.75.0.0/16", null, "Amazon.com", "Amazon.com", "Hosting");
        var location = new Location("North America", "NA", "United States", "US", "Washington", "WA", "Seattle", "98108", "37.751", "-97.822", "America/Chicago", null);
        var detections = new Detections(true, false, false, false, false, false, true, 60, 100, null, null);
        var ip = new IpResult("1.2.3.4", network, location, null, detections, null, null, null, null);

        assertEquals(RiskLevel.HIGH, ip.riskLevel());
        assertEquals(AccessRecommendation.DENY, ip.accessRecommendation());
        assertTrue(ip.isThreat());
        assertEquals("US", ip.countryCode());
        assertEquals("Amazon.com", ip.provider());
    }

    @Test
    void convenienceAccessorsWithNullNested() {
        var ip = new IpResult("1.2.3.4", null, null, null, null, null, null, null, null);

        assertNull(ip.riskLevel());
        assertNull(ip.accessRecommendation());
        assertFalse(ip.isThreat());
        assertNull(ip.countryCode());
        assertNull(ip.provider());
    }
}
