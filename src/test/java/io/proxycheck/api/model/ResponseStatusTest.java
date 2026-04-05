package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseStatusTest {

    @Test
    void httpCodes() {
        assertEquals(java.util.List.of(200), ResponseStatus.OK.httpCodes());
        assertEquals(java.util.List.of(200), ResponseStatus.WARNING.httpCodes());
        assertEquals(java.util.List.of(429, 401, 403), ResponseStatus.DENIED.httpCodes());
        assertEquals(java.util.List.of(400), ResponseStatus.ERROR.httpCodes());
    }

    @Test
    void isSuccessful() {
        assertTrue(ResponseStatus.OK.isSuccessful());
        assertTrue(ResponseStatus.WARNING.isSuccessful());
        assertFalse(ResponseStatus.DENIED.isSuccessful());
        assertFalse(ResponseStatus.ERROR.isSuccessful());
    }
}
