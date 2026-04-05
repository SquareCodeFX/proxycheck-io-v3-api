package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TagEntryTest {

    @Test
    void constructorAndAccessors() {
        var types = new TagTypes(11, 4, 4, 3, null);
        var addresses = Map.of("1.2.3.4", 4, "5.6.7.8", 3);
        var entry = new TagEntry(types, addresses);

        assertEquals(11, entry.types().total());
        assertEquals(4, entry.types().proxy());
        assertEquals(4, entry.types().vpn());
        assertEquals(3, entry.types().rule());
        assertNull(entry.types().blacklist());
        assertEquals(4, entry.addresses().get("1.2.3.4"));
    }

    @Test
    void nullAddressesAllowed() {
        var types = new TagTypes(5, 5, null, null, null);
        var entry = new TagEntry(types, null);
        assertNull(entry.addresses());
    }

    @Test
    void tagTypesEquality() {
        var a = new TagTypes(10, 3, 7, null, null);
        var b = new TagTypes(10, 3, 7, null, null);
        assertEquals(a, b);
    }
}
