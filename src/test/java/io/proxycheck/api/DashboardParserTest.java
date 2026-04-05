package io.proxycheck.api;

import io.proxycheck.api.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DashboardParserTest {

    // -------------------------------------------------------------------------
    // Detections
    // -------------------------------------------------------------------------

    @Test
    void parseDetectionsReturnsEntries() {
        String json = """
                {
                    "status": "ok",
                    "1": {
                        "time formatted": "23rd of November 2017 at 7:01 am",
                        "time raw": "1511420515",
                        "address": "177.154.145.103",
                        "detection type": "vpn",
                        "answering node": "ATLAS",
                        "tag": "Login Attempt"
                    },
                    "2": {
                        "time formatted": "24th of November 2017 at 8:00 am",
                        "time raw": "1511510400",
                        "address": "1.2.3.4",
                        "detection type": "proxy",
                        "answering node": "BRAVO",
                        "tag": ""
                    }
                }
                """;

        List<Detection> detections = DashboardParser.parseDetections(json);

        assertEquals(2, detections.size());
        Detection d1 = detections.get(0);
        assertEquals("177.154.145.103", d1.address());
        assertEquals("vpn", d1.detectionType());
        assertEquals("ATLAS", d1.answeringNode());
        assertEquals("Login Attempt", d1.tag());
        assertEquals("1511420515", d1.timeRaw());
        assertEquals("23rd of November 2017 at 7:01 am", d1.timeFormatted());

        Detection d2 = detections.get(1);
        assertEquals("proxy", d2.detectionType());
    }

    @Test
    void parseDetectionsSkipsMetaKeys() {
        String json = """
                {
                    "status": "ok",
                    "message": "some message",
                    "node": "ATLAS",
                    "1": {
                        "time formatted": "1st",
                        "time raw": "123",
                        "address": "1.1.1.1",
                        "detection type": "vpn",
                        "answering node": "A",
                        "tag": "test"
                    }
                }
                """;

        List<Detection> detections = DashboardParser.parseDetections(json);
        assertEquals(1, detections.size());
    }

    @Test
    void parseDetectionsEmptyResponse() {
        String json = """
                {
                    "status": "ok"
                }
                """;
        List<Detection> detections = DashboardParser.parseDetections(json);
        assertTrue(detections.isEmpty());
    }

    @Test
    void parseDetectionsOrderedByNumericKey() {
        // Keys deliberately out of lexicographic order to confirm integer sorting
        String json = """
                {
                    "10": {
                        "time formatted": "tenth",
                        "time raw": "10",
                        "address": "10.0.0.1",
                        "detection type": "vpn",
                        "answering node": "X",
                        "tag": ""
                    },
                    "2": {
                        "time formatted": "second",
                        "time raw": "2",
                        "address": "2.0.0.1",
                        "detection type": "proxy",
                        "answering node": "Y",
                        "tag": ""
                    },
                    "1": {
                        "time formatted": "first",
                        "time raw": "1",
                        "address": "1.0.0.1",
                        "detection type": "proxy",
                        "answering node": "Z",
                        "tag": ""
                    }
                }
                """;

        List<Detection> detections = DashboardParser.parseDetections(json);
        assertEquals(3, detections.size());
        // Must be sorted 1, 2, 10 — not lexicographic order (1, 10, 2)
        assertEquals("1.0.0.1", detections.get(0).address());
        assertEquals("2.0.0.1", detections.get(1).address());
        assertEquals("10.0.0.1", detections.get(2).address());
    }

    // -------------------------------------------------------------------------
    // Tags
    // -------------------------------------------------------------------------

    @Test
    void parseTagsReturnsMappedEntries() {
        String json = """
                {
                    "www.example.com/wp-login.php": {
                        "types": {
                            "total": 11,
                            "proxy": 4,
                            "vpn": 4,
                            "rule": 3
                        },
                        "addresses": {
                            "149.28.76.201": 4,
                            "87.107.18.84": 4,
                            "62.210.83.206": 3
                        }
                    },
                    "www.example.com/register": {
                        "types": {
                            "total": 10,
                            "proxy": 7,
                            "vpn": 3
                        }
                    }
                }
                """;

        Map<String, TagEntry> tags = DashboardParser.parseTags(json);
        assertEquals(2, tags.size());

        TagEntry loginEntry = tags.get("www.example.com/wp-login.php");
        assertNotNull(loginEntry);
        assertEquals(11, loginEntry.types().total());
        assertEquals(4, loginEntry.types().proxy());
        assertEquals(4, loginEntry.types().vpn());
        assertEquals(3, loginEntry.types().rule());
        assertNotNull(loginEntry.addresses());
        assertEquals(4, loginEntry.addresses().get("149.28.76.201"));

        TagEntry registerEntry = tags.get("www.example.com/register");
        assertNull(registerEntry.addresses());
        assertEquals(10, registerEntry.types().total());
    }

    @Test
    void parseTagsSkipsMetaKeys() {
        String json = """
                {
                    "status": "ok",
                    "message": "ok",
                    "my-tag": {
                        "types": {"total": 5, "proxy": 5}
                    }
                }
                """;
        Map<String, TagEntry> tags = DashboardParser.parseTags(json);
        assertEquals(1, tags.size());
        assertNotNull(tags.get("my-tag"));
    }

    // -------------------------------------------------------------------------
    // Account usage
    // -------------------------------------------------------------------------

    @Test
    void parseAccountUsage() {
        String json = """
                {
                    "Burst Tokens Available": 1,
                    "Burst Token Allowance": 1,
                    "Queries Today": "255",
                    "Daily Limit": "1000",
                    "Queries Total": "98523",
                    "Plan Tier": "Free"
                }
                """;

        AccountUsage usage = DashboardParser.parseAccountUsage(json);
        assertEquals(1, usage.burstTokensAvailable());
        assertEquals(1, usage.burstTokenAllowance());
        assertEquals("255", usage.queriesToday());
        assertEquals("1000", usage.dailyLimit());
        assertEquals("98523", usage.queriesTotal());
        assertEquals("Free", usage.planTier());
    }

    // -------------------------------------------------------------------------
    // Query volume
    // -------------------------------------------------------------------------

    @Test
    void parseQueryVolumeReturnsDailyEntries() {
        String json = """
                {
                    "2025-09-07": {
                        "total_queries": 55115,
                        "positive_queries": 7535,
                        "negative_queries": 47547,
                        "refused_queries": 0,
                        "proxy": 99,
                        "vpn": 7490,
                        "compromised": 1345,
                        "scraper": 2,
                        "tor": 0,
                        "hosting": 7494,
                        "disposable_email": 1,
                        "reusable_email": 33,
                        "custom_rule": 0,
                        "blacklisted": 0
                    },
                    "2025-09-08": {
                        "total_queries": 100,
                        "positive_queries": 10,
                        "negative_queries": 90,
                        "refused_queries": 0,
                        "proxy": 5,
                        "vpn": 5,
                        "compromised": 0,
                        "scraper": 0,
                        "tor": 0,
                        "hosting": 0,
                        "disposable_email": 0,
                        "reusable_email": 0,
                        "custom_rule": 0,
                        "blacklisted": 0
                    }
                }
                """;

        Map<String, QueryVolume> volume = DashboardParser.parseQueryVolume(json);
        assertEquals(2, volume.size());

        QueryVolume day = volume.get("2025-09-07");
        assertNotNull(day);
        assertEquals(55115, day.totalQueries());
        assertEquals(7535, day.positiveQueries());
        assertEquals(47547, day.negativeQueries());
        assertEquals(99, day.proxy());
        assertEquals(7490, day.vpn());
        assertEquals(1345, day.compromised());
        assertEquals(1, day.disposableEmail());
        assertEquals(33, day.reusableEmail());
    }

    @Test
    void parseQueryVolumeSkipsMetaKeys() {
        String json = """
                {
                    "status": "ok",
                    "2025-01-01": {
                        "total_queries": 10,
                        "positive_queries": 1,
                        "negative_queries": 9,
                        "refused_queries": 0,
                        "proxy": 0,
                        "vpn": 1,
                        "compromised": 0,
                        "scraper": 0,
                        "tor": 0,
                        "hosting": 0,
                        "disposable_email": 0,
                        "reusable_email": 0,
                        "custom_rule": 0,
                        "blacklisted": 0
                    }
                }
                """;
        Map<String, QueryVolume> volume = DashboardParser.parseQueryVolume(json);
        assertEquals(1, volume.size());
    }

    // -------------------------------------------------------------------------
    // List names
    // -------------------------------------------------------------------------

    @Test
    void parseListNamesReturnsNonMetaKeys() {
        String json = """
                {
                    "status": "ok",
                    "whitelist": 5,
                    "blacklist": 12,
                    "custom-list": 0
                }
                """;
        List<String> names = DashboardParser.parseListNames(json);
        assertEquals(3, names.size());
        assertTrue(names.contains("whitelist"));
        assertTrue(names.contains("blacklist"));
        assertTrue(names.contains("custom-list"));
    }

    // -------------------------------------------------------------------------
    // List entries
    // -------------------------------------------------------------------------

    @Test
    void parseListEntriesFromNumericKeys() {
        String json = """
                {
                    "status": "ok",
                    "1": "1.2.3.4",
                    "2": "5.6.7.8"
                }
                """;
        List<String> entries = DashboardParser.parseListEntries(json);
        assertEquals(2, entries.size());
        assertTrue(entries.contains("1.2.3.4"));
        assertTrue(entries.contains("5.6.7.8"));
    }

    @Test
    void parseListEntriesFromJsonArray() {
        String json = """
                ["1.2.3.4", "5.6.7.8", "9.10.11.12"]
                """;
        List<String> entries = DashboardParser.parseListEntries(json);
        assertEquals(3, entries.size());
        assertEquals("1.2.3.4", entries.get(0));
    }

    // -------------------------------------------------------------------------
    // Rules
    // -------------------------------------------------------------------------

    @Test
    void parseRulesReturnIdNameMap() {
        String json = """
                {
                    "status": "ok",
                    "4o4h6107e089": "Elevate Risk Score",
                    "abc123def456": "Block Tor Exits"
                }
                """;
        Map<String, String> rules = DashboardParser.parseRules(json);
        assertEquals(2, rules.size());
        assertEquals("Elevate Risk Score", rules.get("4o4h6107e089"));
        assertEquals("Block Tor Exits", rules.get("abc123def456"));
    }

    // -------------------------------------------------------------------------
    // Dashboard response (mutations)
    // -------------------------------------------------------------------------

    @Test
    void parseDashboardResponseStandardFormat() {
        String json = """
                {
                    "status": "ok",
                    "message": "List has been added to successfully."
                }
                """;
        DashboardResponse response = DashboardParser.parseDashboardResponse(json);
        assertEquals("ok", response.status());
        assertEquals("List has been added to successfully.", response.message());
        assertTrue(response.isOk());
        assertNull(response.count());
    }

    @Test
    void parseDashboardResponseCorsFormat() {
        String json = """
                {
                    "success": "Your origins have been updated.",
                    "origin_count": 5
                }
                """;
        DashboardResponse response = DashboardParser.parseDashboardResponse(json);
        assertEquals("ok", response.status());
        assertEquals("Your origins have been updated.", response.message());
        assertEquals(5, response.count());
        assertTrue(response.isOk());
    }

    @Test
    void parseDashboardResponseErrorStatus() {
        String json = """
                {
                    "status": "denied",
                    "message": "No API key was supplied."
                }
                """;
        DashboardResponse response = DashboardParser.parseDashboardResponse(json);
        assertEquals("denied", response.status());
        assertFalse(response.isOk());
    }

    @Test
    void parseDashboardResponseWithOriginCount() {
        String json = """
                {
                    "status": "ok",
                    "message": "Origins cleared.",
                    "origin_count": 0
                }
                """;
        DashboardResponse response = DashboardParser.parseDashboardResponse(json);
        assertTrue(response.isOk());
        assertEquals(0, response.count());
    }
}
