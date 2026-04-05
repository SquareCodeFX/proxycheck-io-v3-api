package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AttackHistoryTest {

    @Test
    void totalAttacks() {
        var history = new AttackHistory(Map.of(
                "login_attempt", 5,
                "registration_attempt", 3
        ));
        assertEquals(8, history.totalAttacks());
    }

    @Test
    void totalAttacksEmpty() {
        assertEquals(0, new AttackHistory(Map.of()).totalAttacks());
    }

    @Test
    void totalAttacksNull() {
        assertEquals(0, new AttackHistory(null).totalAttacks());
    }
}
