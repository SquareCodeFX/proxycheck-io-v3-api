package io.proxycheck.api;

import io.proxycheck.api.model.ProxyCheckResponse;
import io.proxycheck.api.model.ResponseStatus;
import io.proxycheck.api.model.StatusMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseParserTest {

    @Test
    void parseIpResponse() {
        String json = """
                {
                    "status": "ok",
                    "98.75.2.4": {
                        "network": {
                            "asn": "AS16509",
                            "range": "98.75.0.0/16",
                            "hostname": "db1-production-newyork-dc-2.company.net",
                            "provider": "Amazon.com, Inc.",
                            "organisation": "Amazon.com, Inc.",
                            "type": "Hosting"
                        },
                        "location": {
                            "continent_name": "North America",
                            "continent_code": "NA",
                            "country_name": "United States",
                            "country_code": "US",
                            "region_name": "Washington",
                            "region_code": "WA",
                            "city_name": "Seattle",
                            "postal_code": "98108",
                            "latitude": "37.751",
                            "longitude": "-97.822",
                            "timezone": "America/Chicago",
                            "currency": {
                                "code": "USD",
                                "name": "Dollar",
                                "symbol": "$"
                            }
                        },
                        "device_estimate": {
                            "address": 3,
                            "subnet": 38
                        },
                        "detections": {
                            "proxy": false,
                            "vpn": false,
                            "compromised": false,
                            "scraper": false,
                            "tor": false,
                            "hosting": true,
                            "anonymous": false,
                            "risk": 33,
                            "confidence": 100,
                            "first_seen": null,
                            "last_seen": null
                        },
                        "detection_history": null,
                        "attack_history": null,
                        "operator": null,
                        "last_updated": "2026-03-26T10:40:08Z"
                    },
                    "query_time": 5
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertEquals(ResponseStatus.OK, response.status());
        assertNull(response.message());
        assertNull(response.node());
        assertEquals(5, response.queryTime());
        assertEquals(1, response.ipResults().size());
        assertTrue(response.emailResults().isEmpty());
        assertTrue(response.isOk());

        var ip = response.ipResults().get("98.75.2.4");
        assertNotNull(ip);
        assertEquals("AS16509", ip.network().asn());
        assertEquals("98.75.0.0/16", ip.network().range());
        assertEquals("Hosting", ip.network().type());
        assertEquals("North America", ip.location().continentName());
        assertEquals("US", ip.location().countryCode());
        assertEquals("Seattle", ip.location().cityName());
        assertEquals("USD", ip.location().currency().code());
        assertEquals(3, ip.deviceEstimate().address());
        assertEquals(38, ip.deviceEstimate().subnet());
        assertFalse(ip.detections().proxy());
        assertTrue(ip.detections().hosting());
        assertEquals(33, ip.detections().risk());
        assertEquals(100, ip.detections().confidence());
        assertEquals("2026-03-26T10:40:08Z", ip.lastUpdated());

        assertTrue(response.firstIpResult().isPresent());
        assertEquals(ip, response.firstIpResult().get());
        assertTrue(response.firstEmailResult().isEmpty());
    }

    @Test
    void parseWarningResponse() {
        String json = """
                {
                    "status": "warning",
                    "message": "Your API Key has been disabled for a violation of our terms of service.",
                    "98.75.2.4": {
                        "network": { "asn": "AS16509", "range": "98.75.0.0/16", "hostname": null, "provider": "Amazon.com, Inc.", "organisation": "Amazon.com, Inc.", "type": "Hosting" },
                        "location": { "continent_name": "North America", "continent_code": "NA", "country_name": "United States", "country_code": "US", "region_name": "Washington", "region_code": "WA", "city_name": "Seattle", "postal_code": "98108", "latitude": "37.751", "longitude": "-97.822", "timezone": "America/Chicago", "currency": { "code": "USD", "name": "Dollar", "symbol": "$" } },
                        "device_estimate": { "address": 3, "subnet": 38 },
                        "detections": { "proxy": false, "vpn": false, "compromised": false, "scraper": false, "tor": false, "hosting": true, "anonymous": false, "risk": 33, "confidence": 100, "first_seen": null, "last_seen": null },
                        "detection_history": null,
                        "attack_history": null,
                        "operator": null,
                        "last_updated": "2026-03-26T10:40:46Z"
                    },
                    "query_time": 5
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertEquals(ResponseStatus.WARNING, response.status());
        assertTrue(response.isWarning());
        assertEquals("Your API Key has been disabled for a violation of our terms of service.", response.message());
        assertEquals(1, response.ipResults().size());
    }

    @Test
    void parseDeniedResponse() {
        String json = """
                {
                    "status": "denied",
                    "message": "Your access to the API has been blocked due to using a proxy server to perform your query. Please signup for an account to re-enable access by proxy."
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertEquals(ResponseStatus.DENIED, response.status());
        assertTrue(response.isDenied());
        assertNotNull(response.message());
        assertTrue(response.ipResults().isEmpty());
        assertTrue(response.emailResults().isEmpty());
    }

    @Test
    void parseErrorResponse() {
        String json = """
                {
                    "status": "error",
                    "message": "No valid IP Addresses supplied."
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.isError());
        assertEquals("No valid IP Addresses supplied.", response.message());
    }

    @Test
    void parseEmailResponse() {
        String json = """
                {
                    "status": "ok",
                    "john.smith@gmail.com": {
                        "disposable": false
                    }
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertEquals(ResponseStatus.OK, response.status());
        assertTrue(response.ipResults().isEmpty());
        assertEquals(1, response.emailResults().size());

        var email = response.emailResults().get("john.smith@gmail.com");
        assertNotNull(email);
        assertFalse(email.disposable());

        assertTrue(response.firstEmailResult().isPresent());
        assertEquals(email, response.firstEmailResult().get());
        assertTrue(response.firstIpResult().isEmpty());
    }

    @Test
    void parseDisposableEmailResponse() {
        String json = """
                {
                    "status": "ok",
                    "n87hynhwsa@temp-mail.org": {
                        "disposable": true
                    }
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        var email = response.emailResults().get("n87hynhwsa@temp-mail.org");
        assertNotNull(email);
        assertTrue(email.disposable());
    }

    @Test
    void parseShortIpResponse() {
        String json = """
                {
                    "status": "ok",
                    "node": "VEGA",
                    "query_time": 5,
                    "address": "98.75.2.4",
                    "network": {
                        "asn": "AS16509",
                        "range": "98.75.0.0/16",
                        "hostname": "db1-production-newyork-dc-2.company.net",
                        "provider": "Amazon.com, Inc.",
                        "organisation": "Amazon.com, Inc.",
                        "type": "Hosting"
                    },
                    "location": {
                        "continent_name": "North America",
                        "continent_code": "NA",
                        "country_name": "United States",
                        "country_code": "US",
                        "region_name": "Washington",
                        "region_code": "WA",
                        "city_name": "Seattle",
                        "postal_code": "98108",
                        "latitude": "37.751",
                        "longitude": "-97.822",
                        "timezone": "America/Chicago",
                        "currency": {
                            "code": "USD",
                            "name": "Dollar",
                            "symbol": "$"
                        }
                    },
                    "device_estimate": {
                        "address": 3,
                        "subnet": 38
                    },
                    "detections": {
                        "proxy": false,
                        "vpn": false,
                        "compromised": false,
                        "scraper": false,
                        "tor": false,
                        "hosting": true,
                        "anonymous": false,
                        "risk": 33,
                        "confidence": 100,
                        "first_seen": null,
                        "last_seen": null
                    },
                    "detection_history": null,
                    "attack_history": null,
                    "operator": null,
                    "last_updated": "2026-03-26T10:42:47Z"
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, true);

        assertEquals(ResponseStatus.OK, response.status());
        assertEquals("VEGA", response.node());
        assertEquals(5, response.queryTime());
        assertEquals(1, response.ipResults().size());

        var ip = response.ipResults().get("98.75.2.4");
        assertNotNull(ip);
        assertEquals("98.75.2.4", ip.address());
        assertEquals("AS16509", ip.network().asn());
        assertTrue(ip.detections().hosting());
    }

    @Test
    void parseShortEmailResponse() {
        String json = """
                {
                    "status": "ok",
                    "node": "NOVA",
                    "address": "john.smith@gmail.com",
                    "disposable": false
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, true);

        assertEquals(ResponseStatus.OK, response.status());
        assertEquals("NOVA", response.node());
        assertEquals(1, response.emailResults().size());

        var email = response.emailResults().get("john.smith@gmail.com");
        assertNotNull(email);
        assertFalse(email.disposable());
    }

    @Test
    void parseShortEmailWarningResponse() {
        String json = """
                {
                    "status": "warning",
                    "message": "Your API Key has been disabled for a violation of our terms of service.",
                    "node": "ASTRO",
                    "address": "john.smith@gmail.com",
                    "disposable": false
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, true);

        assertEquals(ResponseStatus.WARNING, response.status());
        assertTrue(response.isWarning());
        assertEquals("ASTRO", response.node());
        assertEquals(1, response.emailResults().size());
    }

    @Test
    void parseNodeWithoutShort() {
        String json = """
                {
                    "status": "ok",
                    "node": "JUPITER",
                    "98.75.2.4": {
                        "network": { "asn": "AS16509", "range": "98.75.0.0/16", "hostname": null, "provider": "Amazon.com, Inc.", "organisation": "Amazon.com, Inc.", "type": "Hosting" },
                        "location": { "continent_name": "North America", "continent_code": "NA", "country_name": "United States", "country_code": "US", "region_name": "Washington", "region_code": "WA", "city_name": "Seattle", "postal_code": "98108", "latitude": "37.751", "longitude": "-97.822", "timezone": "America/Chicago", "currency": { "code": "USD", "name": "Dollar", "symbol": "$" } },
                        "device_estimate": { "address": 3, "subnet": 38 },
                        "detections": { "proxy": false, "vpn": false, "compromised": false, "scraper": false, "tor": false, "hosting": true, "anonymous": false, "risk": 33, "confidence": 100, "first_seen": null, "last_seen": null },
                        "detection_history": null,
                        "attack_history": null,
                        "operator": null,
                        "last_updated": "2026-03-26T10:43:18Z"
                    },
                    "query_time": 5
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertEquals("JUPITER", response.node());
        assertEquals(1, response.ipResults().size());
        assertNotNull(response.ipResults().get("98.75.2.4"));
    }

    @Test
    void parseMixedIpAndEmailResponse() {
        String json = """
                {
                    "status": "ok",
                    "1.2.3.4": {
                        "detections": { "proxy": true, "vpn": false, "compromised": false, "scraper": false, "tor": false, "hosting": false, "anonymous": true, "risk": 66, "confidence": 95, "first_seen": null, "last_seen": null }
                    },
                    "test@example.com": {
                        "disposable": true
                    },
                    "query_time": 3
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertEquals(1, response.ipResults().size());
        assertEquals(1, response.emailResults().size());
        assertTrue(response.ipResults().get("1.2.3.4").detections().isProxy());
        assertTrue(response.emailResults().get("test@example.com").isDisposable());
    }

    @Test
    void parseEmptyOkResponse() {
        String json = """
                {
                    "status": "ok",
                    "query_time": 0
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertTrue(response.isOk());
        assertEquals(0, response.totalResults());
    }

    @Test
    void parseResponseWithoutStatus() {
        String json = """
                {
                    "query_time": 1
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertNull(response.status());
        assertEquals(1, response.queryTime());
    }

    @Test
    void parseFullResponseWithOperatorAndHistory() {
        String json = """
                {
                    "status": "ok",
                    "37.60.48.2": {
                        "network": {
                            "asn": "AS9009",
                            "range": "37.60.48.0/24",
                            "hostname": null,
                            "provider": "M247 Europe SRL",
                            "organisation": "IVPN Limited",
                            "type": "Hosting"
                        },
                        "location": {
                            "continent_name": "Europe",
                            "continent_code": "EU",
                            "country_name": "Romania",
                            "country_code": "RO",
                            "region_name": null,
                            "region_code": null,
                            "city_name": "Bucharest",
                            "postal_code": null,
                            "latitude": "44.4268",
                            "longitude": "26.1025",
                            "timezone": "Europe/Bucharest",
                            "currency": { "code": "RON", "name": "Leu", "symbol": "lei" }
                        },
                        "device_estimate": { "address": 1, "subnet": 12 },
                        "detections": {
                            "proxy": false,
                            "vpn": true,
                            "compromised": false,
                            "scraper": false,
                            "tor": false,
                            "hosting": true,
                            "anonymous": true,
                            "risk": 50,
                            "confidence": 100,
                            "first_seen": "2025-11-20T08:00:00Z",
                            "last_seen": "2026-03-26T10:00:00Z"
                        },
                        "detection_history": {
                            "delisted": false,
                            "delist_datetime": "2026-04-02T10:00:00Z"
                        },
                        "attack_history": {
                            "login_attempt": 5,
                            "registration_attempt": 2
                        },
                        "operator": {
                            "name": "IVPN",
                            "url": "https://www.ivpn.net/",
                            "anonymity": "high",
                            "popularity": "medium",
                            "services": ["datacenter_vpns"],
                            "protocols": ["WireGuard", "OpenVPN", "IPSec", "IKEv2"],
                            "policies": {
                                "ad_filtering": true,
                                "free_access": false,
                                "paid_access": true,
                                "port_forwarding": false,
                                "logging": false,
                                "anonymous_payments": true,
                                "crypto_payments": true,
                                "traceable_ownership": true
                            },
                            "additional_operators": null
                        },
                        "last_updated": "2026-03-26T10:40:08Z"
                    },
                    "query_time": 7
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertEquals(ResponseStatus.OK, response.status());
        var ip = response.ipResults().get("37.60.48.2");
        assertNotNull(ip);

        // Operator
        assertNotNull(ip.operator());
        assertEquals("IVPN", ip.operator().name());
        assertEquals("https://www.ivpn.net/", ip.operator().url());
        assertEquals("high", ip.operator().anonymity());
        assertEquals("medium", ip.operator().popularity());
        assertEquals(1, ip.operator().services().size());
        assertEquals("datacenter_vpns", ip.operator().services().get(0));
        assertEquals(4, ip.operator().protocols().size());
        assertTrue(ip.operator().policies().adFiltering());
        assertFalse(ip.operator().policies().freeAccess());
        assertNull(ip.operator().additionalOperators());

        // Detection history
        assertNotNull(ip.detectionHistory());
        assertFalse(ip.detectionHistory().delisted());
        assertEquals("2026-04-02T10:00:00Z", ip.detectionHistory().delistDatetime());

        // Attack history
        assertNotNull(ip.attackHistory());
        assertEquals(7, ip.attackHistory().totalAttacks());
        assertEquals(5, ip.attackHistory().attacks().get("login_attempt"));
        assertEquals(2, ip.attackHistory().attacks().get("registration_attempt"));

        // Network type
        assertEquals(io.proxycheck.api.model.NetworkType.HOSTING, ip.networkType());

        // Detections
        assertTrue(ip.detections().isVpn());
        assertTrue(ip.detections().isAnonymous());
        assertEquals("2025-11-20T08:00:00Z", ip.detections().firstSeen());
        assertEquals("2026-03-26T10:00:00Z", ip.detections().lastSeen());
    }

    @Test
    void parseEmailWithStringDisposable() {
        String json = """
                {
                    "status": "ok",
                    "john.smith@gmail.com": {
                        "disposable": "no"
                    }
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);
        var email = response.emailResults().get("john.smith@gmail.com");
        assertNotNull(email);
        assertFalse(email.isDisposable());
    }

    @Test
    void parseEmailWithStringDisposableYes() {
        String json = """
                {
                    "status": "ok",
                    "trash@temp.org": {
                        "disposable": "yes"
                    }
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);
        var email = response.emailResults().get("trash@temp.org");
        assertNotNull(email);
        assertTrue(email.isDisposable());
    }

    @Test
    void parseEmailWithDisposableNestedInDetections() {
        String json = """
                {
                    "status": "ok",
                    "paxoge2268@cosdas.com": {
                        "detections": {
                            "disposable": true
                        }
                    },
                    "query_time": 2
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);
        assertTrue(response.ipResults().isEmpty());
        assertEquals(1, response.emailResults().size());

        var email = response.emailResults().get("paxoge2268@cosdas.com");
        assertNotNull(email);
        assertTrue(email.isDisposable());
    }

    @Test
    void parseEmailWithDisposableFalseNestedInDetections() {
        String json = """
                {
                    "status": "ok",
                    "user@gmail.com": {
                        "detections": {
                            "disposable": false
                        }
                    }
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);
        var email = response.emailResults().get("user@gmail.com");
        assertNotNull(email);
        assertFalse(email.isDisposable());
    }

    @Test
    void parseEmailWithDisposableStringNestedInDetections() {
        String json = """
                {
                    "status": "ok",
                    "user@gmail.com": {
                        "detections": {
                            "disposable": "no"
                        }
                    }
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);
        var email = response.emailResults().get("user@gmail.com");
        assertNotNull(email);
        assertFalse(email.isDisposable());
    }

    @Test
    void parseWarningWithStatusMessage() {
        String json = """
                {
                    "status": "warning",
                    "message": "Your API Key has been disabled for a violation of our terms of service.",
                    "john.smith@gmail.com": {
                        "disposable": false
                    }
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertTrue(response.isWarning());
        assertTrue(response.isSuccessful());
        assertEquals("Your API Key has been disabled for a violation of our terms of service.", response.message());
        assertEquals(StatusMessage.API_KEY_DISABLED, response.statusMessage());
        assertEquals(1, response.emailResults().size());
        assertFalse(response.emailResults().get("john.smith@gmail.com").isDisposable());
    }

    @Test
    void parseNullMessageAndNode() {
        String json = """
                {
                    "status": "ok",
                    "message": null,
                    "node": null,
                    "query_time": null
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        assertEquals(ResponseStatus.OK, response.status());
        assertNull(response.message());
        assertNull(response.node());
        assertNull(response.queryTime());
    }

    @Test
    void parseMixedIpAndEmailBatchWithWarning() {
        String json = """
                {
                    "status": "warning",
                    "message": "You are within 10% of your query limit for the day.",
                    "89.1.208.179": {
                        "network": { "asn": "AS8422", "range": "89.1.208.179/28", "hostname": null, "provider": "NetCologne", "organisation": "NetCologne", "type": "Residential" },
                        "detections": { "proxy": false, "vpn": false, "compromised": false, "scraper": false, "tor": false, "hosting": false, "anonymous": false, "risk": 0, "confidence": 100, "first_seen": null, "last_seen": null }
                    },
                    "paxoge2268@cosdas.com": {
                        "detections": {
                            "disposable": true
                        }
                    },
                    "query_time": 6
                }
                """;

        ProxyCheckResponse response = ResponseParser.parse(json, false);

        // Status & message
        assertTrue(response.isWarning());
        assertEquals(StatusMessage.NEAR_QUERY_LIMIT, response.statusMessage());

        // IP result
        assertEquals(1, response.ipResults().size());
        var ip = response.ipResults().get("89.1.208.179");
        assertNotNull(ip);
        assertFalse(ip.isThreat());
        assertEquals("Residential", ip.network().type());

        // Email result (nested disposable)
        assertEquals(1, response.emailResults().size());
        var email = response.emailResults().get("paxoge2268@cosdas.com");
        assertNotNull(email);
        assertTrue(email.isDisposable());

        // Total
        assertEquals(2, response.totalResults());
        assertEquals(6, response.queryTime());
    }
}
