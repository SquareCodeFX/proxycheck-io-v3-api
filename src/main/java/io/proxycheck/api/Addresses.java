package io.proxycheck.api;

import java.util.regex.Pattern;

/**
 * Utility for validating IP addresses and email formats before sending to the API.
 * These are client-side validations for fast feedback; the API performs its own validation.
 */
public final class Addresses {

    // Validates each octet is 0-255 with no leading zeros ambiguity
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    // Covers common IPv6 forms: full, loopback (::), compressed (::1, fe80::1)
    // Does not cover IPv4-mapped (::ffff:x.x.x.x) or zone IDs (%eth0)
    private static final Pattern IPV6 = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|" +
            "^::$|" +
            "^([0-9a-fA-F]{1,4}:){1,7}:$|" +
            "^::[0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4}){0,5}$|" +
            "^[0-9a-fA-F]{1,4}::[0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4}){0,4}$|" +
            "^([0-9a-fA-F]{1,4}:){2}:[0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4}){0,3}$|" +
            "^([0-9a-fA-F]{1,4}:){1,5}:[0-9a-fA-F]{1,4}$");

    // Basic RFC 5322 simplified email pattern; sufficient for pre-flight validation
    private static final Pattern EMAIL = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private Addresses() {}

    public static boolean isValidIpv4(String address) {
        return address != null && IPV4.matcher(address).matches();
    }

    public static boolean isValidIpv6(String address) {
        return address != null && IPV6.matcher(address).matches();
    }

    public static boolean isValidIp(String address) {
        return isValidIpv4(address) || isValidIpv6(address);
    }

    public static boolean isValidEmail(String address) {
        return address != null && EMAIL.matcher(address).matches();
    }

    public static boolean isValid(String address) {
        return isValidIp(address) || isValidEmail(address);
    }

    public static void requireValid(String address) {
        if (!isValid(address)) {
            throw new IllegalArgumentException("Invalid address: " + address);
        }
    }
}
