package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountUsageTest {

    @Test
    void constructorAndAccessors() {
        var usage = new AccountUsage(1, 1, "255", "1000", "98523", "Free");

        assertEquals(1, usage.burstTokensAvailable());
        assertEquals(1, usage.burstTokenAllowance());
        assertEquals("255", usage.queriesToday());
        assertEquals("1000", usage.dailyLimit());
        assertEquals("98523", usage.queriesTotal());
        assertEquals("Free", usage.planTier());
    }

    @Test
    void equalityBasedOnComponents() {
        var a = new AccountUsage(2, 5, "100", "1000", "50000", "Starter");
        var b = new AccountUsage(2, 5, "100", "1000", "50000", "Starter");
        assertEquals(a, b);
    }
}
