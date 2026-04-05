package io.proxycheck.api;

import io.proxycheck.api.exception.ProxyCheckException;
import io.proxycheck.api.model.*;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * Comprehensive example demonstrating every feature of the proxycheck.io v3 API client.
 *
 * <p>Covers: single/batch/async checks, IPv4, IPv6, email, query flags, response parsing,
 * network/location/detections/operator/history sections, risk scores, confidence,
 * caching, rate limiting, retry, whitelist, listeners, pattern matching, and filtering.
 */
public class Example {

    private static final String API_KEY = "YOUR_API_KEY";

    public static void main(String[] args) throws Exception {

        // =====================================================================
        // 1. CLIENT CREATION
        // =====================================================================

        // Simple: just an API key
        try (var simple = ProxyCheckClient.of(API_KEY)) {
            System.out.println("Simple client created");
        }

        // No-retry client
        try (var noRetryClient = ProxyCheckClient.builder()
                .apiKey(API_KEY).noRetry().build()) {
            System.out.println("No-retry client created");
        }

        // Client with custom HttpClient
        try (var customHttp = ProxyCheckClient.builder()
                .apiKey(API_KEY)
                .httpClient(HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build())
                .noRetry()
                .build()) {
            System.out.println("Custom HttpClient created");
        }

        // Cache with default max size (1000)
        try (var cachedClient = ProxyCheckClient.builder()
                .apiKey(API_KEY).cache(Duration.ofMinutes(10)).noRetry().build()) {
            System.out.println("Cached client (default size) created");
        }

        // Whitelist from Collection
        try (var wlClient = ProxyCheckClient.builder()
                .apiKey(API_KEY)
                .whitelist(List.of("10.0.0.1", "10.0.0.2"))
                .noRetry().build()) {
            System.out.println("Whitelist from Collection created");
        }

        // Full builder: cache, rate limit, retry, whitelist, listener
        try (var client = ProxyCheckClient.builder()
                .apiKey(API_KEY)
                .timeout(Duration.ofSeconds(15))
                .cache(Duration.ofMinutes(5), 2000)
                .rateLimitPerSecond(150)
                .retryPolicy(RetryPolicy.builder()
                        .maxRetries(3)
                        .initialDelay(Duration.ofMillis(500))
                        .multiplier(2.0)
                        .maxDelay(Duration.ofSeconds(8))
                        .build())
                .whitelist("127.0.0.1", "::1")
                .listener(CheckListener.builder()
                        .onRequest(addrs -> System.out.println("[Event] Checking: " + addrs))
                        .onResponse((addrs, resp) -> System.out.println("[Event] Got " + resp.totalResults() + " results"))
                        .onError((addrs, err) -> System.err.println("[Event] Error: " + err.getMessage()))
                        .onCacheHit(addr -> System.out.println("[Event] Cache hit: " + addr))
                        .onRetry((attempt, cause) -> System.out.println("[Event] Retry #" + attempt))
                        .build())
                .build()) {

            // =================================================================
            // 2. SINGLE IPv4 CHECK
            // =================================================================

            System.out.println("\n--- Single IPv4 Check ---");
            var ipv4Response = client.check("83.214.56.129");
            printResponseMeta(ipv4Response);
            ipv4Response.firstIpResult().ifPresent(Example::printFullIpResult);

            // =================================================================
            // 3. SINGLE IPv6 CHECK
            // =================================================================

            System.out.println("\n--- Single IPv6 Check ---");
            var ipv6Response = client.check("2607:f8b0:400a:80b::200e");
            ipv6Response.firstIpResult().ifPresent(ip -> {
                System.out.println("  Address: " + ip.address());
                System.out.println("  Provider: " + ip.provider());
                System.out.println("  Country: " + (ip.location() != null ? ip.location().countryName() : "n/a"));
            });

            // =================================================================
            // 4. SINGLE EMAIL CHECK
            // =================================================================

            System.out.println("\n--- Single Email Check ---");
            var emailResponse = client.check("squarecodefx@gmail.com");
            emailResponse.firstEmailResult().ifPresent(email -> {
                System.out.println("  Email: " + email.address());
                System.out.println("  Disposable: " + email.isDisposable());
            });

            // =================================================================
            // 5. QUERY FLAGS
            // =================================================================

            System.out.println("\n--- Query Flags ---");

            // All individual flags
            var flagsResponse = client.check("172.94.201.17", QueryFlags.create()
                    .node(true)          // &node=1 - show cluster node
                    .prettyPrint(false)   // &p=0    - compact JSON
                    .days(7)             // &days=7 - detections within 7 days
                    .tag("example-app")  // &tag=example-app - tag query
                    .ver("11-February-2026")); // &ver=... - API version
            System.out.println("  Node: " + flagsResponse.node());
            System.out.println("  Query time: " + flagsResponse.queryTime() + "ms");

            // Short mode - flat response format
            var shortResponse = client.check("45.188.73.204", QueryFlags.create()
                    .shortResponse(true));
            shortResponse.firstIpResult().ifPresent(ip ->
                    System.out.println("  Short mode - Address: " + ip.address()));

            // Preset flags
            client.check("198.51.244.92", QueryFlags.detailed());   // node=1, days=7
            client.check("109.67.13.155", QueryFlags.minimal());    // short=1
            client.check("83.214.56.129", QueryFlags.withNode());   // node=1

            // Disable logging with noTag()
            client.check("172.94.201.17", QueryFlags.create().noTag()); // &tag=0

            // =================================================================
            // 6. BATCH CHECK - MIXED IPs AND EMAILS
            // =================================================================

            System.out.println("\n--- Batch Check (Mixed IPs + Emails) ---");
            var batchResponse = client.check(List.of(
                    "83.214.56.129",
                    "172.94.201.17",
                    "45.188.73.204",
                    "admin@squarecode.de",
                    "squarecodefx@gmail.com"
            ));
            System.out.println("  Total results: " + batchResponse.totalResults());
            System.out.println("  IP results: " + batchResponse.ipResults().size());
            System.out.println("  Email results: " + batchResponse.emailResults().size());

            // checkMultiple varargs shorthand
            var multiResponse = client.checkMultiple(
                    "83.214.56.129", "172.94.201.17", "info@prohosting24.de");
            System.out.println("  checkMultiple results: " + multiResponse.totalResults());

            // Batch with flags
            var batchFlagged = client.check(List.of(
                    "198.51.244.92", "109.67.13.155"
            ), QueryFlags.create().days(3).node(true));
            System.out.println("  Batch with flags - node: " + batchFlagged.node());

            // =================================================================
            // 7. ASYNC CHECKS
            // =================================================================

            System.out.println("\n--- Async Checks ---");

            // Single async
            var future1 = client.checkAsync("83.214.56.129");

            // Batch async
            var future2 = client.checkAsync(List.of(
                    "2a03:7c40:8f1e:4a21:9c6d:1b2f:7a44:8e12",
                    "2001:db8:4c2f:8a10:7d33:bb2a:6c90:1f55"
            ));

            // Async with flags
            var future3 = client.checkAsync("172.94.201.17", QueryFlags.create().days(5));

            // Async batch with flags
            var future4 = client.checkAsync(
                    List.of("198.51.244.92", "109.67.13.155"),
                    QueryFlags.create().days(3));

            // Wait for all results
            var asyncResult1 = future1.get();
            var asyncResult2 = future2.get();
            var asyncResult3 = future3.get();
            var asyncResult4 = future4.get();
            System.out.println("  Async single: " + asyncResult1.totalResults() + " results");
            System.out.println("  Async batch: " + asyncResult2.totalResults() + " results");
            System.out.println("  Async flagged: " + asyncResult3.totalResults() + " results");
            System.out.println("  Async batch+flags: " + asyncResult4.totalResults() + " results");

            // =================================================================
            // 8. RESPONSE STATUS & STATUS MESSAGES
            // =================================================================

            System.out.println("\n--- Response Status ---");
            var resp = client.check("83.214.56.129");
            System.out.println("  Status: " + resp.status());         // OK, WARNING, DENIED, ERROR
            System.out.println("  isOk: " + resp.isOk());
            System.out.println("  isWarning: " + resp.isWarning());
            System.out.println("  isDenied: " + resp.isDenied());
            System.out.println("  isError: " + resp.isError());
            System.out.println("  isSuccessful: " + resp.isSuccessful()); // OK or WARNING
            System.out.println("  Message: " + resp.message());

            // Parse known status messages
            StatusMessage msg = resp.statusMessage();
            if (msg != null) {
                System.out.println("  Known message: " + msg.name());
                System.out.println("  Expected status: " + msg.status());
            }

            // =================================================================
            // 9. NETWORK SECTION
            // =================================================================

            System.out.println("\n--- Network Section ---");
            client.check("83.214.56.129").firstIpResult().ifPresent(ip -> {
                Network net = ip.network();
                if (net != null) {
                    System.out.println("  ASN: " + net.asn());
                    System.out.println("  Range: " + net.range());
                    System.out.println("  Hostname: " + net.hostname());       // may be null
                    System.out.println("  Provider: " + net.provider());
                    System.out.println("  Organisation: " + net.organisation());
                    System.out.println("  Type (string): " + net.type());       // "Hosting", "Residential", etc.
                    System.out.println("  Type (enum): " + net.networkType());  // NetworkType.HOSTING etc.
                }
                // Convenience accessor on IpResult
                System.out.println("  networkType(): " + ip.networkType());
            });

            // NetworkType enum values
            System.out.println("  All network types: ");
            for (NetworkType type : NetworkType.values()) {
                System.out.println("    " + type + " = \"" + type.apiValue() + "\"");
            }
            System.out.println("    null = unknown");

            // =================================================================
            // 10. LOCATION SECTION
            // =================================================================

            System.out.println("\n--- Location Section ---");
            client.check("83.214.56.129").firstIpResult().ifPresent(ip -> {
                Location loc = ip.location();
                if (loc != null) {
                    System.out.println("  Continent: " + loc.continentName() + " (" + loc.continentCode() + ")");
                    System.out.println("  Country: " + loc.countryName() + " (" + loc.countryCode() + ")");
                    System.out.println("  Region: " + loc.regionName() + " (" + loc.regionCode() + ")");
                    System.out.println("  City: " + loc.cityName());
                    System.out.println("  Postal: " + loc.postalCode());
                    System.out.println("  Lat/Lon: " + loc.latitude() + ", " + loc.longitude());
                    System.out.println("  Timezone: " + loc.timezone());
                    if (loc.currency() != null) {
                        System.out.println("  Currency: " + loc.currency().name()
                                + " (" + loc.currency().code() + ", " + loc.currency().symbol() + ")");
                    }
                }
                // Convenience accessor
                System.out.println("  countryCode(): " + ip.countryCode());
            });

            // =================================================================
            // 11. DEVICE ESTIMATE SECTION
            // =================================================================

            System.out.println("\n--- Device Estimate ---");
            client.check("83.214.56.129").firstIpResult().ifPresent(ip -> {
                DeviceEstimate dev = ip.deviceEstimate();
                if (dev != null) {
                    System.out.println("  Devices behind this IP: " + dev.address());
                    System.out.println("  Devices in subnet: " + dev.subnet());
                }
            });

            // =================================================================
            // 12. DETECTIONS SECTION
            // =================================================================

            System.out.println("\n--- Detections ---");
            client.check("83.214.56.129").firstIpResult().ifPresent(ip -> {
                Detections det = ip.detections();
                if (det != null) {
                    // Boolean detection types (multiple can be true simultaneously)
                    System.out.println("  Proxy: " + det.isProxy());
                    System.out.println("  VPN: " + det.isVpn());
                    System.out.println("  TOR: " + det.isTor());
                    System.out.println("  Compromised: " + det.isCompromised());
                    System.out.println("  Scraper: " + det.isScraper());
                    System.out.println("  Hosting: " + det.isHosting());
                    System.out.println("  Anonymous: " + det.isAnonymous());

                    // Aggregated threat check (proxy || vpn || tor || compromised || scraper)
                    System.out.println("  isThreat(): " + det.isThreat());

                    // Risk score (0-100)
                    System.out.println("  Risk: " + det.risk());
                    System.out.println("  RiskLevel: " + det.riskLevel());

                    // Confidence score (0-100)
                    System.out.println("  Confidence: " + det.confidence());

                    // First/last seen (ISO 8601 or null)
                    System.out.println("  First seen: " + det.firstSeen());
                    System.out.println("  Last seen: " + det.lastSeen());
                }
            });

            // =================================================================
            // 13. RISK SCORE & ACCESS RECOMMENDATION
            // =================================================================

            System.out.println("\n--- Risk Score & Access Recommendation ---");
            client.check("83.214.56.129").firstIpResult().ifPresent(ip -> {
                // RiskLevel: LOW (0-25), MEDIUM (26-50), HIGH (51-75), VERY_HIGH (76-100)
                RiskLevel level = ip.riskLevel();
                System.out.println("  Risk level: " + level);
                if (level != null) {
                    System.out.println("  Score range: " + level.minScore() + "-" + level.maxScore());
                }

                // AccessRecommendation: ALLOW, CHALLENGE, DENY
                // Based on risk score + anonymous flag (matches API docs table)
                AccessRecommendation rec = ip.accessRecommendation();
                System.out.println("  Recommendation: " + rec);
            });

            // Manual evaluation
            System.out.println("  Risk 10, anon=false: " + AccessRecommendation.evaluate(10, false));  // ALLOW
            System.out.println("  Risk 10, anon=true:  " + AccessRecommendation.evaluate(10, true));   // CHALLENGE
            System.out.println("  Risk 40, anon=false: " + AccessRecommendation.evaluate(40, false));  // CHALLENGE
            System.out.println("  Risk 60, anon=true:  " + AccessRecommendation.evaluate(60, true));   // DENY
            System.out.println("  Risk 90, anon=false: " + AccessRecommendation.evaluate(90, false));  // DENY

            // =================================================================
            // 14. DETECTION HISTORY
            // =================================================================

            System.out.println("\n--- Detection History ---");
            client.check("83.214.56.129").firstIpResult().ifPresent(ip -> {
                DetectionHistory hist = ip.detectionHistory();
                if (hist != null) {
                    // delisted=false: still listed, datetime = planned delist date
                    // delisted=true: already delisted, datetime = when it was delisted
                    System.out.println("  Delisted: " + hist.delisted());
                    System.out.println("  Datetime: " + hist.delistDatetime());
                } else {
                    System.out.println("  No detection history");
                }
            });

            // =================================================================
            // 15. ATTACK HISTORY
            // =================================================================

            System.out.println("\n--- Attack History ---");
            client.check("83.214.56.129").firstIpResult().ifPresent(ip -> {
                AttackHistory attacks = ip.attackHistory();
                if (attacks != null && attacks.attacks() != null) {
                    attacks.attacks().forEach((type, count) ->
                            System.out.println("  " + type + ": " + count));
                    System.out.println("  Total attacks: " + attacks.totalAttacks());
                } else {
                    System.out.println("  No attack history");
                }
            });

            // =================================================================
            // 16. OPERATOR SECTION
            // =================================================================

            System.out.println("\n--- Operator ---");
            client.check("83.214.56.129").firstIpResult().ifPresent(ip -> {
                Operator op = ip.operator();
                if (op != null) {
                    System.out.println("  Name: " + op.name());
                    System.out.println("  URL: " + op.url());
                    System.out.println("  Anonymity: " + op.anonymity());
                    System.out.println("  Popularity: " + op.popularity());
                    System.out.println("  Services: " + op.services());
                    System.out.println("  Protocols: " + op.protocols());

                    // Policies
                    OperatorPolicies pol = op.policies();
                    if (pol != null) {
                        System.out.println("  Policies:");
                        System.out.println("    Ad filtering: " + pol.adFiltering());
                        System.out.println("    Free access: " + pol.freeAccess());
                        System.out.println("    Paid access: " + pol.paidAccess());
                        System.out.println("    Port forwarding: " + pol.portForwarding());
                        System.out.println("    Logging: " + pol.logging());
                        System.out.println("    Anonymous payments: " + pol.anonymousPayments());
                        System.out.println("    Crypto payments: " + pol.cryptoPayments());
                        System.out.println("    Traceable ownership: " + pol.traceableOwnership());
                    }

                    // Additional operators
                    if (op.additionalOperators() != null) {
                        op.additionalOperators().forEach(extra ->
                                System.out.println("  Additional operator: " + extra.name()));
                    }
                } else {
                    System.out.println("  operator: null (no operator data)");
                }
            });

            // =================================================================
            // 17. last_updated FIELD
            // =================================================================

            System.out.println("\n--- Last Updated ---");
            client.check("83.214.56.129").firstIpResult().ifPresent(ip ->
                    System.out.println("  Last updated (ISO 8601): " + ip.lastUpdated()));

            // =================================================================
            // 18. RESULT FILTERING & STREAMING
            // =================================================================

            System.out.println("\n--- Filtering & Streaming ---");
            var mixedResponse = client.check(List.of(
                    "83.214.56.129", "172.94.201.17", "45.188.73.204",
                    "admin@squarecode.de", "info@prohosting24.de"
            ));

            // Threat vs safe IPs
            System.out.println("  Threat IPs: " + mixedResponse.threatIps().size());
            System.out.println("  Safe IPs: " + mixedResponse.safeIps().size());
            System.out.println("  Has any threat: " + mixedResponse.hasThreat());

            // Disposable vs legitimate emails
            System.out.println("  Disposable emails: " + mixedResponse.disposableEmails().size());
            System.out.println("  Legitimate emails: " + mixedResponse.legitimateEmails().size());
            System.out.println("  Has disposable: " + mixedResponse.hasDisposableEmail());

            // Custom IP filter
            List<IpResult> hostingIps = mixedResponse.ipResultsMatching(
                    ip -> ip.networkType() == NetworkType.HOSTING);
            System.out.println("  Hosting IPs: " + hostingIps.size());

            // Lookup by address
            mixedResponse.ipResult("83.214.56.129").ifPresent(ip ->
                    System.out.println("  Lookup 83.214.56.129: " + ip.provider()));
            mixedResponse.emailResult("admin@squarecode.de").ifPresent(email ->
                    System.out.println("  Lookup admin@squarecode.de: disposable=" + email.isDisposable()));

            // Unified result lookup (IP or email)
            mixedResponse.result("83.214.56.129").ifPresent(r ->
                    System.out.println("  Unified lookup: " + r.address()));

            // Pattern matching on sealed Result type
            System.out.println("  All results via pattern matching:");
            mixedResponse.streamResults().forEach(result -> {
                switch (result) {
                    case IpResult ip -> System.out.println("    IP: " + ip.address()
                            + " threat=" + ip.isThreat());
                    case EmailResult email -> System.out.println("    Email: " + email.address()
                            + " disposable=" + email.isDisposable());
                }
            });

            // =================================================================
            // 19. WHITELIST
            // =================================================================

            System.out.println("\n--- Whitelist ---");
            // 127.0.0.1 and ::1 were whitelisted in the builder
            var whitelistResponse = client.check("127.0.0.1");
            System.out.println("  Whitelisted 127.0.0.1: " + (whitelistResponse == ProxyCheckResponse.empty()));

            // =================================================================
            // 20. CACHE
            // =================================================================

            System.out.println("\n--- Cache ---");
            // First call: API request (listener will print "[Event] Checking: ...")
            client.check("198.51.244.92");
            // Second call: cache hit (listener will print "[Event] Cache hit: ...")
            client.check("198.51.244.92");

            // Cache management
            client.invalidateCache("198.51.244.92");
            client.clearCache();

            // =================================================================
            // 21. ADDRESS VALIDATION
            // =================================================================

            System.out.println("\n--- Address Validation ---");
            System.out.println("  IPv4 valid: " + Addresses.isValidIpv4("83.214.56.129"));
            System.out.println("  IPv6 valid: " + Addresses.isValidIpv6("2607:f8b0:400a:80b::200e"));
            System.out.println("  IP valid: " + Addresses.isValidIp("172.94.201.17"));
            System.out.println("  Email valid: " + Addresses.isValidEmail("admin@squarecode.de"));
            System.out.println("  Any valid: " + Addresses.isValid("squarecodefx@gmail.com"));
            System.out.println("  Invalid: " + Addresses.isValid("not-an-address"));

            // requireValid throws IllegalArgumentException for invalid addresses
            try {
                Addresses.requireValid("not-valid");
            } catch (IllegalArgumentException e) {
                System.out.println("  requireValid threw: " + e.getMessage());
            }

            // =================================================================
            // 22. DYNAMIC LISTENER MANAGEMENT
            // =================================================================

            System.out.println("\n--- Dynamic Listener Management ---");
            var dynamicListener = new CheckListener() {
                @Override
                public void onRequest(java.util.Collection<String> addresses) {
                    System.out.println("  [Dynamic] Request: " + addresses);
                }
            };
            client.addListener(dynamicListener);
            client.check("109.67.13.155"); // triggers dynamic listener
            boolean removed = client.removeListener(dynamicListener);
            System.out.println("  Listener removed: " + removed);
            client.check("109.67.13.155"); // dynamic listener no longer fires

            // =================================================================
            // 23. ERROR HANDLING & ProxyCheckException
            // =================================================================

            System.out.println("\n--- Error Handling ---");
            try {
                // This would throw if the API returns 5xx
                client.check("83.214.56.129");
            } catch (ProxyCheckException e) {
                System.out.println("  Message: " + e.getMessage());
                System.out.println("  Has HTTP code: " + e.hasHttpStatusCode());
                if (e.hasHttpStatusCode()) {
                    System.out.println("  HTTP code: " + e.httpStatusCode());
                }
                System.out.println("  Cause: " + e.getCause());
            }

            // HTTP codes per response status (from API docs)
            System.out.println("  OK HTTP codes: " + ResponseStatus.OK.httpCodes());           // [200]
            System.out.println("  WARNING HTTP codes: " + ResponseStatus.WARNING.httpCodes());  // [200]
            System.out.println("  DENIED HTTP codes: " + ResponseStatus.DENIED.httpCodes());    // [429, 401, 403]
            System.out.println("  ERROR HTTP codes: " + ResponseStatus.ERROR.httpCodes());      // [400]

            // =================================================================
            // 24. TYPED STREAMS
            // =================================================================

            System.out.println("\n--- Typed Streams ---");
            var streamResponse = client.check(List.of(
                    "83.214.56.129", "172.94.201.17", "admin@squarecode.de"));

            // Stream only IP results
            System.out.println("  IP stream:");
            streamResponse.streamIpResults().forEach(ip ->
                    System.out.println("    " + ip.address() + " risk=" + (ip.detections() != null ? ip.detections().risk() : "n/a")));

            // Stream only email results
            System.out.println("  Email stream:");
            streamResponse.streamEmailResults().forEach(email ->
                    System.out.println("    " + email.address() + " disposable=" + email.isDisposable()));

            // =================================================================
            // 25. MANUAL ENUM LOOKUPS
            // =================================================================

            System.out.println("\n--- Manual Enum Lookups ---");

            // RiskLevel from score
            System.out.println("  RiskLevel.fromScore(10): " + RiskLevel.fromScore(10));   // LOW
            System.out.println("  RiskLevel.fromScore(40): " + RiskLevel.fromScore(40));   // MEDIUM
            System.out.println("  RiskLevel.fromScore(60): " + RiskLevel.fromScore(60));   // HIGH
            System.out.println("  RiskLevel.fromScore(90): " + RiskLevel.fromScore(90));   // VERY_HIGH

            // NetworkType from API string
            System.out.println("  NetworkType.fromApiValue(\"Hosting\"): " + NetworkType.fromApiValue("Hosting"));
            System.out.println("  NetworkType.fromApiValue(\"Residential\"): " + NetworkType.fromApiValue("Residential"));
            System.out.println("  NetworkType.fromApiValue(null): " + NetworkType.fromApiValue(null));
            System.out.println("  NetworkType.fromApiValue(\"Unknown\"): " + NetworkType.fromApiValue("Unknown"));

            // StatusMessage from API message string
            System.out.println("  StatusMessage.fromMessage(\"You're sending more than 175 requests per second.\"): "
                    + StatusMessage.fromMessage("You're sending more than 175 requests per second."));
            System.out.println("  StatusMessage.fromMessage(\"unknown\"): "
                    + StatusMessage.fromMessage("unknown"));
        }

        System.out.println("\nAll examples completed.");
    }

    // --- Helper methods ---

    private static void printResponseMeta(ProxyCheckResponse response) {
        System.out.println("  Status: " + response.status());
        System.out.println("  Message: " + response.message());
        System.out.println("  Node: " + response.node());
        System.out.println("  Query time: " + response.queryTime() + "ms");
        System.out.println("  Total results: " + response.totalResults());
    }

    private static void printFullIpResult(IpResult ip) {
        System.out.println("  === Full IP Result for " + ip.address() + " ===");

        // Network
        if (ip.network() != null) {
            System.out.println("  Network: " + ip.network().asn() + " " + ip.network().provider()
                    + " [" + ip.network().type() + "]");
        }

        // Location
        if (ip.location() != null) {
            System.out.println("  Location: " + ip.location().cityName() + ", "
                    + ip.location().countryName());
        }

        // Device estimate
        if (ip.deviceEstimate() != null) {
            System.out.println("  Devices: " + ip.deviceEstimate().address()
                    + " (subnet: " + ip.deviceEstimate().subnet() + ")");
        }

        // Detections
        if (ip.detections() != null) {
            System.out.println("  Threat: " + ip.isThreat()
                    + " | Risk: " + ip.detections().risk()
                    + " | Confidence: " + ip.detections().confidence());
        }

        // Detection history
        if (ip.detectionHistory() != null) {
            System.out.println("  Detection history: delisted=" + ip.detectionHistory().delisted()
                    + " datetime=" + ip.detectionHistory().delistDatetime());
        }

        // Attack history
        if (ip.attackHistory() != null) {
            System.out.println("  Attack history: " + ip.attackHistory().totalAttacks() + " total attacks");
        }

        // Operator
        if (ip.operator() != null) {
            System.out.println("  Operator: " + ip.operator().name()
                    + " (services: " + ip.operator().services() + ")");
        }

        // Last updated
        System.out.println("  Last updated: " + ip.lastUpdated());
    }
}
