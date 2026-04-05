package io.proxycheck.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryFlagsTest {

    @Test
    void emptyFlags() {
        var flags = QueryFlags.create();
        assertEquals("", flags.toQueryString());
    }

    @Test
    void allFlags() {
        var flags = QueryFlags.create()
                .node(true)
                .shortResponse(true)
                .prettyPrint(false)
                .days(7)
                .tag("msg")
                .ver("10-October-2025");

        String qs = flags.toQueryString();
        assertTrue(qs.contains("node=1"));
        assertTrue(qs.contains("short=1"));
        assertTrue(qs.contains("p=0"));
        assertTrue(qs.contains("days=7"));
        assertTrue(qs.contains("tag=msg"));
        assertTrue(qs.contains("ver=10-October-2025"));
        assertTrue(qs.startsWith("&"));
    }

    @Test
    void noTag() {
        var flags = QueryFlags.create().noTag();
        assertTrue(flags.toQueryString().contains("tag=0"));
    }

    @Test
    void shortFlag() {
        assertFalse(QueryFlags.create().isShort());
        assertTrue(QueryFlags.create().shortResponse(true).isShort());
        assertFalse(QueryFlags.create().shortResponse(false).isShort());
    }

    @Test
    void invalidDays() {
        assertThrows(IllegalArgumentException.class, () -> QueryFlags.create().days(0));
        assertThrows(IllegalArgumentException.class, () -> QueryFlags.create().days(-1));
    }

    @Test
    void detailedPreset() {
        var flags = QueryFlags.detailed();
        String qs = flags.toQueryString();
        assertTrue(qs.contains("node=1"));
        assertTrue(qs.contains("days=7"));
    }

    @Test
    void minimalPreset() {
        var flags = QueryFlags.minimal();
        assertTrue(flags.isShort());
    }

    @Test
    void withNodePreset() {
        var flags = QueryFlags.withNode();
        assertTrue(flags.toQueryString().contains("node=1"));
        assertFalse(flags.isShort());
    }
}
