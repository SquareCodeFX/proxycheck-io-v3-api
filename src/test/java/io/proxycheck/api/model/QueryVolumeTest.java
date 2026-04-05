package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryVolumeTest {

    @Test
    void constructorAndAccessors() {
        var vol = new QueryVolume(55115, 7535, 47547, 0, 99, 7490, 1345, 2, 0, 7494, 1, 33, 0, 0);

        assertEquals(55115, vol.totalQueries());
        assertEquals(7535, vol.positiveQueries());
        assertEquals(47547, vol.negativeQueries());
        assertEquals(0, vol.refusedQueries());
        assertEquals(99, vol.proxy());
        assertEquals(7490, vol.vpn());
        assertEquals(1345, vol.compromised());
        assertEquals(2, vol.scraper());
        assertEquals(0, vol.tor());
        assertEquals(7494, vol.hosting());
        assertEquals(1, vol.disposableEmail());
        assertEquals(33, vol.reusableEmail());
        assertEquals(0, vol.customRule());
        assertEquals(0, vol.blacklisted());
    }

    @Test
    void equalityBasedOnAllComponents() {
        var a = new QueryVolume(100, 10, 90, 0, 5, 5, 0, 0, 0, 0, 0, 0, 0, 0);
        var b = new QueryVolume(100, 10, 90, 0, 5, 5, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(a, b);
    }
}
