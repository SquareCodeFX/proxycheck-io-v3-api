package io.proxycheck.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * API response status as returned in the JSON {@code "status"} field.
 * Note: both OK and WARNING map to HTTP 200 — the status is distinguished
 * solely by the JSON payload, not the HTTP status code.
 */
public enum ResponseStatus {

    @SerializedName("ok")
    OK(List.of(200)),

    @SerializedName("warning")
    WARNING(List.of(200)),

    @SerializedName("denied")
    DENIED(List.of(429, 401, 403)),

    @SerializedName("error")
    ERROR(List.of(400));

    private final List<Integer> httpCodes;

    ResponseStatus(List<Integer> httpCodes) {
        this.httpCodes = httpCodes;
    }

    public List<Integer> httpCodes() {
        return httpCodes;
    }

    public boolean isSuccessful() {
        return this == OK || this == WARNING;
    }
}
