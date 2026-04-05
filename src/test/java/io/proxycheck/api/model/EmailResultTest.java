package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailResultTest {

    @Test
    void isDisposableTrue() {
        assertTrue(new EmailResult("x@y.com", true).isDisposable());
    }

    @Test
    void isDisposableFalse() {
        assertFalse(new EmailResult("x@y.com", false).isDisposable());
    }

    @Test
    void isDisposableNull() {
        assertFalse(new EmailResult("x@y.com", null).isDisposable());
    }
}
