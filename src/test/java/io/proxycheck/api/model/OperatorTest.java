package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OperatorTest {

    @Test
    void fullOperatorRecord() {
        var policies = new OperatorPolicies(true, false, true, false, false, true, true, true);
        var operator = new Operator(
                "IVPN",
                "https://www.ivpn.net/",
                "high",
                "medium",
                List.of("datacenter_vpns"),
                List.of("WireGuard", "OpenVPN", "IPSec", "IKEv2"),
                policies,
                null
        );

        assertEquals("IVPN", operator.name());
        assertEquals("https://www.ivpn.net/", operator.url());
        assertEquals("high", operator.anonymity());
        assertEquals("medium", operator.popularity());
        assertEquals(List.of("datacenter_vpns"), operator.services());
        assertEquals(4, operator.protocols().size());
        assertTrue(operator.policies().adFiltering());
        assertFalse(operator.policies().freeAccess());
        assertTrue(operator.policies().paidAccess());
        assertFalse(operator.policies().portForwarding());
        assertFalse(operator.policies().logging());
        assertTrue(operator.policies().anonymousPayments());
        assertTrue(operator.policies().cryptoPayments());
        assertTrue(operator.policies().traceableOwnership());
        assertNull(operator.additionalOperators());
    }

    @Test
    void operatorWithAdditionalOperators() {
        var secondary = new Operator("SecondaryVPN", null, "low", "low", null, null, null, null);
        var primary = new Operator("PrimaryVPN", null, "high", "high",
                List.of("residential_proxies"), null, null, List.of(secondary));

        assertEquals(1, primary.additionalOperators().size());
        assertEquals("SecondaryVPN", primary.additionalOperators().get(0).name());
    }
}
