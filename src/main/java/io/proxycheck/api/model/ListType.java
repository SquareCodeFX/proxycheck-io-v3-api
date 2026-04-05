package io.proxycheck.api.model;

/**
 * The classification type assigned to a custom list on proxycheck.io.
 * Supplied as the {@code type} query parameter when creating or modifying lists.
 */
public enum ListType {

    WHITELIST("whitelist"),
    BLACKLIST("blacklist"),
    NONE("none");

    private final String value;

    ListType(String value) {
        this.value = value;
    }

    /** Returns the lowercase string value expected by the API. */
    public String value() {
        return value;
    }
}
