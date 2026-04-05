package io.proxycheck.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressesTest {

    @Test
    void validIpv4() {
        assertTrue(Addresses.isValidIpv4("1.2.3.4"));
        assertTrue(Addresses.isValidIpv4("255.255.255.255"));
        assertTrue(Addresses.isValidIpv4("0.0.0.0"));
        assertTrue(Addresses.isValidIpv4("192.168.1.1"));
    }

    @Test
    void invalidIpv4() {
        assertFalse(Addresses.isValidIpv4("256.1.1.1"));
        assertFalse(Addresses.isValidIpv4("1.2.3"));
        assertFalse(Addresses.isValidIpv4("abc"));
        assertFalse(Addresses.isValidIpv4(null));
        assertFalse(Addresses.isValidIpv4(""));
    }

    @Test
    void validIpv6() {
        assertTrue(Addresses.isValidIpv6("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        assertTrue(Addresses.isValidIpv6("::"));
        assertTrue(Addresses.isValidIpv6("::1"));
    }

    @Test
    void invalidIpv6() {
        assertFalse(Addresses.isValidIpv6("not-ipv6"));
        assertFalse(Addresses.isValidIpv6(null));
    }

    @Test
    void validIp() {
        assertTrue(Addresses.isValidIp("1.2.3.4"));
        assertTrue(Addresses.isValidIp("::1"));
    }

    @Test
    void validEmail() {
        assertTrue(Addresses.isValidEmail("user@example.com"));
        assertTrue(Addresses.isValidEmail("a.b+c@domain.co.uk"));
    }

    @Test
    void invalidEmail() {
        assertFalse(Addresses.isValidEmail("@domain.com"));
        assertFalse(Addresses.isValidEmail("user@"));
        assertFalse(Addresses.isValidEmail("no-at-sign"));
        assertFalse(Addresses.isValidEmail(null));
    }

    @Test
    void isValid() {
        assertTrue(Addresses.isValid("1.2.3.4"));
        assertTrue(Addresses.isValid("user@example.com"));
        assertFalse(Addresses.isValid("not-valid"));
    }

    @Test
    void requireValidThrows() {
        assertDoesNotThrow(() -> Addresses.requireValid("1.2.3.4"));
        assertDoesNotThrow(() -> Addresses.requireValid("user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> Addresses.requireValid("invalid"));
    }
}
