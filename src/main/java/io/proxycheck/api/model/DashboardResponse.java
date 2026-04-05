package io.proxycheck.api.model;

/**
 * Response returned by dashboard mutation operations (add, remove, set, clear, erase,
 * enable, disable, and CORS modifications).
 *
 * @param status  the API status string, typically {@code "ok"} on success or
 *                {@code "denied"} / {@code "error"} on failure
 * @param message the human-readable description of the outcome
 * @param count   optional count returned by some endpoints (e.g. {@code origin_count}
 *                for CORS operations); {@code null} when not provided
 */
public record DashboardResponse(String status, String message, Integer count) {

    /** Returns {@code true} if the operation completed successfully ({@code status == "ok"}). */
    public boolean isOk() {
        return "ok".equalsIgnoreCase(status);
    }
}
