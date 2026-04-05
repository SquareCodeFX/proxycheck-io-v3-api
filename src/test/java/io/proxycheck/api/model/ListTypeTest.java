package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListTypeTest {

    @Test
    void valuesMatchApiStrings() {
        assertEquals("whitelist", ListType.WHITELIST.value());
        assertEquals("blacklist", ListType.BLACKLIST.value());
        assertEquals("none", ListType.NONE.value());
    }

    @Test
    void allThreeValuesExist() {
        assertEquals(3, ListType.values().length);
    }
}
