package io.proxycheck.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.proxycheck.api.model.*;

import java.util.Map;
import java.util.Set;

/**
 * Parses the proxycheck.io JSON response into a {@link ProxyCheckResponse}.
 *
 * <p>The API returns two response formats:
 * <ul>
 *   <li><b>Standard mode:</b> Each result is a nested JSON object keyed by address.</li>
 *   <li><b>Short mode:</b> A flat JSON object with the address inlined as a field.</li>
 * </ul>
 * The parser distinguishes IP results from email results by the presence of
 * the {@code "disposable"} field or an {@code @} in the address key.
 */
final class ResponseParser {

    // Top-level keys that are metadata, not address results.
    // "ip" is present in CORS (client-side) responses and contains the requester's IP.
    private static final Set<String> META_KEYS = Set.of("status", "message", "node", "query_time", "ip");
    private static final Gson GSON = new Gson();

    private ResponseParser() {}

    static ProxyCheckResponse parse(String json, boolean shortMode) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        var builder = new ProxyCheckResponse.Builder();

        if (root.has("status")) {
            builder.status(GSON.fromJson(root.get("status"), ResponseStatus.class));
        }
        // Null-safe reads: the API may return null values for these metadata fields
        String message = getStringOrNull(root, "message");
        if (message != null) {
            builder.message(message);
        }
        String node = getStringOrNull(root, "node");
        if (node != null) {
            builder.node(node);
        }
        if (root.has("query_time") && !root.get("query_time").isJsonNull()) {
            builder.queryTime(root.get("query_time").getAsInt());
        }

        if (shortMode) {
            parseShortResponse(root, builder);
        } else {
            parseStandardResponse(root, builder);
        }

        return builder.build();
    }

    private static void parseShortResponse(JsonObject root, ProxyCheckResponse.Builder builder) {
        if (!root.has("address")) {
            return;
        }
        String address = root.get("address").getAsString();
        if (root.has("disposable") || address.contains("@")) {
            builder.addEmailResult(address, new EmailResult(address, parseDisposable(root)));
        } else {
            builder.addIpResult(address, parseIpFields(root, address));
        }
    }

    // Standard mode: iterate all top-level keys, skip metadata and non-object values,
    // treat each remaining JSON object as an address result.
    // Email results are detected by either the presence of a "disposable" field or
    // an @ in the key (address), since the API may omit the disposable field in
    // mixed IP+email batch requests.
    private static void parseStandardResponse(JsonObject root, ProxyCheckResponse.Builder builder) {
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            if (META_KEYS.contains(key) || !entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject data = entry.getValue().getAsJsonObject();
            if (isEmailResult(key, data)) {
                builder.addEmailResult(key, new EmailResult(key, parseDisposable(data)));
            } else {
                builder.addIpResult(key, parseIpFields(data, key));
            }
        }
    }

    /**
     * Determines if a result is an email check by checking for:
     * 1. A top-level "disposable" field
     * 2. A "disposable" field nested in "detections"
     * 3. An @ in the address key (fallback for mixed batch responses)
     */
    private static boolean isEmailResult(String key, JsonObject data) {
        if (data.has("disposable")) {
            return true;
        }
        if (data.has("detections") && data.get("detections").isJsonObject()
                && data.getAsJsonObject("detections").has("disposable")) {
            return true;
        }
        return key.contains("@");
    }

    private static IpResult parseIpFields(JsonObject obj, String address) {
        return new IpResult(
                address,
                deserialize(obj, "network", Network.class),
                deserialize(obj, "location", Location.class),
                deserialize(obj, "device_estimate", DeviceEstimate.class),
                deserialize(obj, "detections", Detections.class),
                deserialize(obj, "detection_history", DetectionHistory.class),
                parseAttackHistory(obj),
                deserialize(obj, "operator", Operator.class),
                getStringOrNull(obj, "last_updated")
        );
    }

    /**
     * The attack_history section is a flat JSON object with attack type keys and integer counts.
     * We parse it into an {@link AttackHistory} record wrapping a {@code Map<String, Integer>}.
     */
    private static AttackHistory parseAttackHistory(JsonObject obj) {
        if (!obj.has("attack_history") || obj.get("attack_history").isJsonNull()) {
            return null;
        }
        JsonObject attacks = obj.getAsJsonObject("attack_history");
        var map = new java.util.LinkedHashMap<String, Integer>();
        for (Map.Entry<String, JsonElement> entry : attacks.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                map.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }
        return new AttackHistory(map);
    }

    private static <T> T deserialize(JsonObject obj, String key, Class<T> type) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return GSON.fromJson(obj.get(key), type);
    }

    /**
     * Parses the disposable field which may appear at the top level of the result object
     * ({@code "disposable": true}) or nested inside the detections section
     * ({@code "detections": {"disposable": true}}). The value may be a boolean or
     * a string ({@code "yes"}/{@code "no"}) depending on the API version.
     */
    private static boolean parseDisposable(JsonObject obj) {
        // Check top-level first: {"disposable": true}
        if (obj.has("disposable") && !obj.get("disposable").isJsonNull()) {
            return parseBooleanOrString(obj.get("disposable"));
        }
        // Check nested in detections: {"detections": {"disposable": true}}
        if (obj.has("detections") && obj.get("detections").isJsonObject()) {
            JsonObject detections = obj.getAsJsonObject("detections");
            if (detections.has("disposable") && !detections.get("disposable").isJsonNull()) {
                return parseBooleanOrString(detections.get("disposable"));
            }
        }
        return false;
    }

    private static boolean parseBooleanOrString(JsonElement element) {
        if (element.isJsonPrimitive()) {
            var prim = element.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                return prim.getAsBoolean();
            }
            if (prim.isString()) {
                return "yes".equalsIgnoreCase(prim.getAsString());
            }
        }
        return false;
    }

    @SuppressWarnings("SameParameterValue")
    private static String getStringOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
}
