package io.proxycheck.api.model;

/**
 * The allocation type of an IP address as reported by the API.
 */
public enum NetworkType {

    RESIDENTIAL("Residential"),
    BUSINESS("Business"),
    WIRELESS("Wireless"),
    HOSTING("Hosting");

    private final String apiValue;

    NetworkType(String apiValue) {
        this.apiValue = apiValue;
    }

    /**
     * Returns the string value as used in the API response.
     */
    public String apiValue() {
        return apiValue;
    }

    /**
     * Resolves an API string value to a {@code NetworkType}, or {@code null}
     * if the value is unknown or {@code null}.
     */
    public static NetworkType fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (NetworkType type : values()) {
            if (type.apiValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
