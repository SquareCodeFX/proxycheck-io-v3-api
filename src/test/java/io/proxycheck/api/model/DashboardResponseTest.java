package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashboardResponseTest {

    @Test
    void isOkReturnsTrueForOkStatus() {
        var r = new DashboardResponse("ok", "Success", null);
        assertTrue(r.isOk());
    }

    @Test
    void isOkCaseInsensitive() {
        assertTrue(new DashboardResponse("OK", "msg", null).isOk());
        assertTrue(new DashboardResponse("Ok", "msg", null).isOk());
    }

    @Test
    void isOkReturnsFalseForError() {
        assertFalse(new DashboardResponse("denied", "No key", null).isOk());
        assertFalse(new DashboardResponse("error", "Bad request", null).isOk());
    }

    @Test
    void isOkReturnsFalseForNullStatus() {
        assertFalse(new DashboardResponse(null, "msg", null).isOk());
    }

    @Test
    void countIsOptional() {
        var withCount = new DashboardResponse("ok", "updated", 5);
        assertEquals(5, withCount.count());

        var noCount = new DashboardResponse("ok", "done", null);
        assertNull(noCount.count());
    }

    @Test
    void equalityBasedOnComponents() {
        var a = new DashboardResponse("ok", "msg", 3);
        var b = new DashboardResponse("ok", "msg", 3);
        assertEquals(a, b);
    }
}
