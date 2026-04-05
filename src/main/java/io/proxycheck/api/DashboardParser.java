package io.proxycheck.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.proxycheck.api.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Parses JSON responses from the proxycheck.io Dashboard API into typed model objects.
 *
 * <p>Each {@code parse*} method accepts a raw JSON string and returns a strongly
 * typed result. Parsing is lenient: unknown fields are ignored and missing
 * optional fields default to {@code null} or zero.
 */
final class DashboardParser {

    private static final Gson GSON = new Gson();
    private static final Set<String> META_KEYS = Set.of("status", "message", "node", "query_time", "ip");

    private DashboardParser() {}

    // -------------------------------------------------------------------------
    // Detections
    // -------------------------------------------------------------------------

    /**
     * Parses the detection-export response into an ordered list of {@link Detection}
     * entries. Entries are stored under numeric string keys ("1", "2", …); all other
     * top-level keys are treated as metadata and skipped.
     *
     * <p>A {@link TreeMap} keyed by integer index is used to guarantee that detections
     * are returned in the API-assigned order regardless of JSON key insertion order.
     */
    static List<Detection> parseDetections(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        var byIndex = new TreeMap<Integer, Detection>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!isNumericKey(entry.getKey()) || !entry.getValue().isJsonObject()) {
                continue;
            }
            byIndex.put(Integer.parseInt(entry.getKey()),
                    GSON.fromJson(entry.getValue(), Detection.class));
        }
        return Collections.unmodifiableList(new ArrayList<>(byIndex.values()));
    }

    // -------------------------------------------------------------------------
    // Tags
    // -------------------------------------------------------------------------

    /**
     * Parses the tag-export response into a map of tag name → {@link TagEntry}.
     * Metadata keys ("status", "message") are skipped.
     */
    static Map<String, TagEntry> parseTags(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        var map = new LinkedHashMap<String, TagEntry>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (META_KEYS.contains(entry.getKey()) || !entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject obj = entry.getValue().getAsJsonObject();
            TagTypes types = obj.has("types") ? GSON.fromJson(obj.get("types"), TagTypes.class) : null;
            Map<String, Integer> addresses = null;
            if (obj.has("addresses") && obj.get("addresses").isJsonObject()) {
                addresses = parseStringIntMap(obj.getAsJsonObject("addresses"));
            }
            map.put(entry.getKey(), new TagEntry(types, addresses));
        }
        return Collections.unmodifiableMap(map);
    }

    // -------------------------------------------------------------------------
    // Account usage
    // -------------------------------------------------------------------------

    /**
     * Parses the usage-export response into an {@link AccountUsage} record.
     * The response is a flat JSON object with human-readable key names that map
     * to fields via {@code @SerializedName} annotations.
     */
    static AccountUsage parseAccountUsage(String json) {
        return GSON.fromJson(json, AccountUsage.class);
    }

    // -------------------------------------------------------------------------
    // Query volume
    // -------------------------------------------------------------------------

    /**
     * Parses the query-volume export response into a date → {@link QueryVolume} map.
     * Keys are date strings in {@code YYYY-MM-DD} format; metadata keys are skipped.
     */
    static Map<String, QueryVolume> parseQueryVolume(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        var map = new LinkedHashMap<String, QueryVolume>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (META_KEYS.contains(entry.getKey()) || !entry.getValue().isJsonObject()) {
                continue;
            }
            map.put(entry.getKey(), GSON.fromJson(entry.getValue(), QueryVolume.class));
        }
        return Collections.unmodifiableMap(map);
    }

    // -------------------------------------------------------------------------
    // Custom lists
    // -------------------------------------------------------------------------

    /**
     * Parses a list-names response (returned when no specific list is requested).
     * Returns the top-level JSON keys that are not metadata fields.
     */
    static List<String> parseListNames(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        var names = new ArrayList<String>();
        for (String key : root.keySet()) {
            if (!META_KEYS.contains(key)) {
                names.add(key);
            }
        }
        return Collections.unmodifiableList(names);
    }

    /**
     * Parses a specific-list print response into a list of entry strings.
     * Supports both numeric-keyed objects ("1": "entry") and JSON arrays.
     * Metadata keys are skipped.
     */
    static List<String> parseListEntries(String json) {
        JsonElement element = GSON.fromJson(json, JsonElement.class);
        var entries = new ArrayList<String>();

        if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                if (e.isJsonPrimitive()) {
                    entries.add(e.getAsString());
                }
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                if (META_KEYS.contains(key)) {
                    continue;
                }
                JsonElement val = entry.getValue();
                if (isNumericKey(key) && val.isJsonPrimitive()) {
                    entries.add(val.getAsString());
                } else if (!val.isJsonObject() && !val.isJsonArray() && val.isJsonPrimitive()) {
                    // flat key-value: treat the key itself as the entry (e.g. IP addresses)
                    entries.add(key);
                }
            }
        }

        return Collections.unmodifiableList(entries);
    }

    // -------------------------------------------------------------------------
    // Custom rules
    // -------------------------------------------------------------------------

    /**
     * Parses the rules-print response into a map of rule ID/name → rule name.
     * Non-metadata string-valued keys are treated as rule identifiers.
     */
    static Map<String, String> parseRules(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        var map = new LinkedHashMap<String, String>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (META_KEYS.contains(entry.getKey())) {
                continue;
            }
            JsonElement val = entry.getValue();
            if (val.isJsonPrimitive()) {
                map.put(entry.getKey(), val.getAsString());
            } else if (val.isJsonObject()) {
                // rule object with a "name" field
                JsonObject ruleObj = val.getAsJsonObject();
                String name = ruleObj.has("name") ? ruleObj.get("name").getAsString() : entry.getKey();
                map.put(entry.getKey(), name);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    // -------------------------------------------------------------------------
    // CORS origins
    // -------------------------------------------------------------------------

    /**
     * Parses the CORS list response into a list of origin domain strings.
     * Supports both JSON arrays and numeric-keyed objects.
     */
    static List<String> parseCorsOrigins(String json) {
        return parseListEntries(json);
    }

    // -------------------------------------------------------------------------
    // Mutation / action responses
    // -------------------------------------------------------------------------

    /**
     * Parses a dashboard mutation response (add, remove, set, clear, erase, enable,
     * disable, CORS actions, etc.) into a {@link DashboardResponse}.
     *
     * <p>Handles both the standard format {@code {"status":"ok","message":"..."}} and
     * the CORS-specific format {@code {"success":"...","origin_count":5}}.
     */
    static DashboardResponse parseDashboardResponse(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);

        // Standard format: {"status": "ok", "message": "..."}
        if (root.has("status")) {
            String status = root.get("status").getAsString();
            String message = root.has("message") && !root.get("message").isJsonNull()
                    ? root.get("message").getAsString() : null;
            Integer count = root.has("origin_count") ? root.get("origin_count").getAsInt() : null;
            return new DashboardResponse(status, message, count);
        }

        // CORS format: {"success": "...", "origin_count": 5}
        if (root.has("success")) {
            String message = root.get("success").getAsString();
            Integer count = root.has("origin_count") ? root.get("origin_count").getAsInt() : null;
            return new DashboardResponse("ok", message, count);
        }

        // Fallback: treat the entire JSON as an unknown response
        return new DashboardResponse(null, json, null);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isNumericKey(String key) {
        if (key == null || key.isEmpty()) return false;
        for (char c : key.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private static Map<String, Integer> parseStringIntMap(JsonObject obj) {
        var map = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            if (e.getValue().isJsonPrimitive()) {
                map.put(e.getKey(), e.getValue().getAsInt());
            }
        }
        return map;
    }
}
