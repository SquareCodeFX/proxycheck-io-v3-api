package io.proxycheck.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProxyCheckExceptionTest {

    @Test
    void messageOnly() {
        var ex = new ProxyCheckException("test");
        assertEquals("test", ex.getMessage());
        assertEquals(0, ex.httpStatusCode());
        assertFalse(ex.hasHttpStatusCode());
        assertNull(ex.getCause());
    }

    @Test
    void withHttpStatusCode() {
        var ex = new ProxyCheckException("Server error", 500);
        assertEquals("Server error", ex.getMessage());
        assertEquals(500, ex.httpStatusCode());
        assertTrue(ex.hasHttpStatusCode());
    }

    @Test
    void withCause() {
        var cause = new RuntimeException("root");
        var ex = new ProxyCheckException("wrapped", cause);
        assertEquals("wrapped", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals(0, ex.httpStatusCode());
    }

    @Test
    void withStatusCodeAndCause() {
        var cause = new RuntimeException("root");
        var ex = new ProxyCheckException("error", 502, cause);
        assertEquals(502, ex.httpStatusCode());
        assertEquals(cause, ex.getCause());
        assertTrue(ex.hasHttpStatusCode());
    }
}
