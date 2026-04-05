package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DetectionTest {

    @Test
    void constructorAndAccessors() {
        var d = new Detection(
                "23rd of November 2017 at 7:01 am",
                "1511420515",
                "177.154.145.103",
                "vpn",
                "ATLAS",
                "Login Attempt"
        );

        assertEquals("23rd of November 2017 at 7:01 am", d.timeFormatted());
        assertEquals("1511420515", d.timeRaw());
        assertEquals("177.154.145.103", d.address());
        assertEquals("vpn", d.detectionType());
        assertEquals("ATLAS", d.answeringNode());
        assertEquals("Login Attempt", d.tag());
    }

    @Test
    void nullTagIsAllowed() {
        var d = new Detection("now", "0", "1.2.3.4", "proxy", "NODE", null);
        assertNull(d.tag());
    }

    @Test
    void equalityBasedOnComponents() {
        var a = new Detection("t", "0", "1.1.1.1", "vpn", "A", "tag");
        var b = new Detection("t", "0", "1.1.1.1", "vpn", "A", "tag");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
