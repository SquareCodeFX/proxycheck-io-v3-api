package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkTypeTest {

    @Test
    void fromApiValue() {
        assertEquals(NetworkType.RESIDENTIAL, NetworkType.fromApiValue("Residential"));
        assertEquals(NetworkType.BUSINESS, NetworkType.fromApiValue("Business"));
        assertEquals(NetworkType.WIRELESS, NetworkType.fromApiValue("Wireless"));
        assertEquals(NetworkType.HOSTING, NetworkType.fromApiValue("Hosting"));
    }

    @Test
    void fromApiValueCaseInsensitive() {
        assertEquals(NetworkType.HOSTING, NetworkType.fromApiValue("hosting"));
        assertEquals(NetworkType.RESIDENTIAL, NetworkType.fromApiValue("RESIDENTIAL"));
    }

    @Test
    void fromApiValueNull() {
        assertNull(NetworkType.fromApiValue(null));
    }

    @Test
    void fromApiValueUnknown() {
        assertNull(NetworkType.fromApiValue("SomethingNew"));
    }

    @Test
    void apiValue() {
        assertEquals("Residential", NetworkType.RESIDENTIAL.apiValue());
        assertEquals("Hosting", NetworkType.HOSTING.apiValue());
    }

    @Test
    void networkRecordIntegration() {
        var network = new Network("AS16509", "98.75.0.0/16", null, "Amazon.com", "Amazon.com", "Hosting");
        assertEquals(NetworkType.HOSTING, network.networkType());

        var unknown = new Network("AS123", "1.0.0.0/8", null, "Test", "Test", null);
        assertNull(unknown.networkType());
    }
}
