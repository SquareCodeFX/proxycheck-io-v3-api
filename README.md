<p align="center">
  <h1 align="center">ProxyCheck.io V3 API Client for Java</h1>
  <p align="center">
    A modern, production-ready Java client for the <a href="https://proxycheck.io">proxycheck.io</a> v3 API.<br>
    Detect proxies, VPNs, TOR nodes, and disposable emails with confidence.
  </p>
</p>

<p align="center">
  <a href="#installation">Installation</a> &bull;
  <a href="#quick-start">Quick Start</a> &bull;
  <a href="#documentation">Documentation</a> &bull;
  <a href="#contributing">Contributing</a> &bull;
  <a href="#license">License</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21%2B-blue?logo=openjdk&logoColor=white" alt="Java 21+">
  <img src="https://img.shields.io/badge/API-proxycheck.io%20v3-green" alt="proxycheck.io v3">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue" alt="GPL-3.0">
  <img src="https://img.shields.io/badge/tests-164%20passed-brightgreen" alt="Tests">
  <img src="https://img.shields.io/badge/dependencies-1%20(Gson)-orange" alt="Dependencies">
</p>
 
---

## Why This Library?

The proxycheck.io API is powerful but raw HTTP calls leave a lot of boilerplate on your plate: retries, caching, rate limiting, response parsing, error handling. This library wraps all of that into a clean, fluent API that a Java developer can pick up in minutes.

- **Zero boilerplate** &mdash; one line to create a client, one line to check an address.
- **Production-hardened** &mdash; exponential backoff retries, LRU cache, token-bucket rate limiter.
- **Type-safe** &mdash; sealed `Result` types, record-based models, exhaustive `switch` support.
- **Lightweight** &mdash; single runtime dependency (Gson). No frameworks, no reflection magic.

## Features

| Feature | Description |
|---------|-------------|
| **Sync & Async API** | Blocking calls and `CompletableFuture`-based non-blocking calls |
| **Response Caching** | LRU cache with configurable TTL and max size |
| **Rate Limiting** | Token-bucket limiter to stay within API quotas |
| **Exponential Backoff** | Automatic retry for transient network and server failures |
| **Address Whitelist** | Skip API calls entirely for trusted IPs |
| **Event Listeners** | Hook into request, response, error, cache hit, and retry events |
| **Sealed Result Types** | Exhaustive pattern matching with `IpResult` and `EmailResult` |
| **Query Flag Presets** | `detailed()`, `minimal()`, `withNode()` for common configs |
| **Address Validation** | Client-side IPv4, IPv6, and email format validation |
| **Result Filtering** | Built-in methods for threat IPs, safe IPs, disposable emails |
| **Batch Processing** | Check up to 1,000 addresses per request with automatic splitting |
| **JPMS Support** | Proper `module-info.java` for modular Java applications |

## Requirements

- **Java 21** or later
- **Gradle 9+** (included via wrapper) or **Maven 3.8+**

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
			mavenCentral()
			maven { url = uri("https://jitpack.io") }
}

dependencies {
	    implementation("com.github.SquareCodeFX:proxycheck-io-v3-api:0f54d63704")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'com.github.SquareCodeFX:proxycheck-io-v3-api:0f54d63704'
}
```

### Maven

```xml
<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
</repositories>
  
<dependency>
	    <groupId>com.github.SquareCodeFX</groupId>
	    <artifactId>proxycheck-io-v3-api</artifactId>
	    <version>0f54d63704</version>
	</dependency>
```

### Building from Source

```bash
git clone https://github.com/SquareCodeFX/proxycheck-io-v3-api.git
cd proxycheck-io-v3-api
./gradlew build
```

## Quick Start

```java
import io.proxycheck.api.*;
import io.proxycheck.api.model.*;
 
// Create a client (uses sensible defaults: 30s timeout, 3 retries)
try (var client = ProxyCheckClient.of("your-api-key")) {
 
    // Check a single IP
    var response = client.check("8.8.8.8");
    response.firstIpResult().ifPresent(ip -> {
        System.out.println("Threat:  " + ip.isThreat());
        System.out.println("Risk:    " + ip.riskLevel());
        System.out.println("Country: " + ip.countryCode());
    });
 
    // Check an email
    var emailResponse = client.check("user@tempmail.org");
    emailResponse.firstEmailResult().ifPresent(email ->
        System.out.println("Disposable: " + email.isDisposable()));
}
```

## Documentation

### Table of Contents

- [Client Configuration](#client-configuration)
- [Checking Addresses](#checking-addresses)
- [Query Flags](#query-flags)
- [Working with Responses](#working-with-responses)
- [IP Result Details](#ip-result-details)
- [Email Result Details](#email-result-details)
- [Risk Assessment](#risk-assessment)
- [Caching](#caching)
- [Rate Limiting](#rate-limiting)
- [Retry Policy](#retry-policy)
- [Whitelist](#whitelist)
- [Event Listeners](#event-listeners)
- [Address Validation](#address-validation)
- [Error Handling](#error-handling)
- [Java Module System](#java-module-system)

---

### Client Configuration

Use `ProxyCheckClient.of(key)` for quick setup, or the builder for full control:

```java
var client = ProxyCheckClient.builder()
    .apiKey("your-api-key")
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
        .onRequest(addrs -> log.info("Checking: {}", addrs))
        .onResponse((addrs, resp) -> log.info("Status: {}", resp.status()))
        .onError((addrs, err) -> log.error("Failed: {}", err.getMessage()))
        .onCacheHit(addr -> log.debug("Cache hit: {}", addr))
        .onRetry((attempt, cause) -> log.warn("Retry #{}", attempt))
        .build())
    .build();
```

#### Builder Reference

| Option | Default | Description |
|--------|---------|-------------|
| `apiKey(String)` | *required* | Your proxycheck.io API key |
| `timeout(Duration)` | 30s | HTTP request timeout |
| `httpClient(HttpClient)` | built-in HTTP/2 | Custom `java.net.http.HttpClient` instance |
| `cache(Duration)` | disabled | Enable LRU cache with TTL (default max: 1,000 entries) |
| `cache(Duration, int)` | disabled | Enable LRU cache with TTL and custom max size |
| `rateLimitPerSecond(int)` | disabled | Token-bucket rate limit (recommended: 150) |
| `retryPolicy(RetryPolicy)` | 3 retries, exponential backoff | Custom retry behavior |
| `noRetry()` | &mdash; | Disable all retry attempts |
| `whitelist(String...)` | empty | Addresses that bypass the API entirely |
| `whitelist(Collection)` | empty | Addresses that bypass the API entirely |
| `listener(CheckListener)` | none | Lifecycle event listener |
 
---

### Checking Addresses

#### Single Address

```java
// Default flags
var response = client.check("8.8.8.8");
 
// With query flags
var response = client.check("8.8.8.8", QueryFlags.detailed());
```

#### Batch Check

Batch requests use HTTP POST and automatically split into groups of 1,000 when needed:

```java
// From a collection
var response = client.check(List.of(
    "8.8.8.8", "1.1.1.1", "user@example.com"
));
 
// Varargs shorthand
var response = client.checkMultiple("8.8.8.8", "1.1.1.1");
 
// With flags
var response = client.check(
    List.of("8.8.8.8", "1.1.1.1"),
    QueryFlags.create().days(7).node(true)
);
```

#### Async Check

All check methods have async variants returning `CompletableFuture`:

```java
// Single
client.checkAsync("8.8.8.8")
    .thenAccept(response -> {
        if (response.hasThreat()) {
            System.out.println("Threat detected!");
        }
    });
 
// Batch
client.checkAsync(List.of("8.8.8.8", "1.1.1.1"))
    .thenAccept(response -> response.threatIps().forEach(ip ->
        System.out.println("Blocked: " + ip.address())));
 
// With flags
client.checkAsync("8.8.8.8", QueryFlags.detailed())
    .thenAccept(this::processResponse);
```
 
---

### Query Flags

Control the level of detail the API returns:

```java
// Built-in presets
QueryFlags.detailed()    // node info + 7-day history
QueryFlags.minimal()     // short (flat) response format
QueryFlags.withNode()    // include cluster node identifier
 
// Custom combination
var flags = QueryFlags.create()
    .node(true)
    .days(30)
    .tag("login-flow")
    .ver("11-February-2026");
```

| Method | API Parameter | Description |
|--------|--------------|-------------|
| `node(boolean)` | `node` | Include responding cluster node |
| `shortResponse(boolean)` | `short` | Flat response format |
| `prettyPrint(boolean)` | `p` | Pretty-print JSON |
| `days(int)` | `days` | Historical detection window |
| `tag(String)` | `tag` | Dashboard tracking label |
| `noTag()` | `tag=0` | Disable query logging |
| `ver(String)` | `ver` | Pin API version |
 
---

### Working with Responses

#### Status Checks

```java
var response = client.check("8.8.8.8");
 
response.isOk();          // status == OK
response.isWarning();     // status == WARNING (approaching limits)
response.isDenied();      // status == DENIED (quota exceeded)
response.isError();       // status == ERROR (invalid input)
response.isSuccessful();  // OK or WARNING (results are usable)
 
// Resolve to a known status message enum
StatusMessage msg = response.statusMessage();
if (msg == StatusMessage.NEAR_QUERY_LIMIT) {
    log.warn("Approaching daily query limit");
}
```

#### Accessing Results

```java
// By address
response.ipResult("8.8.8.8").ifPresent(ip -> ...);
response.emailResult("user@example.com").ifPresent(email -> ...);
 
// First result (convenient for single-address checks)
response.firstIpResult().ifPresent(ip -> ...);
response.firstEmailResult().ifPresent(email -> ...);
 
// Unified lookup via the sealed Result interface
response.result("8.8.8.8").ifPresent(result -> {
    switch (result) {
        case IpResult ip       -> handleIp(ip);
        case EmailResult email -> handleEmail(email);
    }
});
```

#### Filtering Results

```java
// Built-in filters
List<IpResult> threats     = response.threatIps();
List<IpResult> safe        = response.safeIps();
List<EmailResult> trash    = response.disposableEmails();
List<EmailResult> legit    = response.legitimateEmails();
 
// Boolean checks
boolean hasThreat     = response.hasThreat();
boolean hasDisposable = response.hasDisposableEmail();
 
// Custom predicate
List<IpResult> highRisk = response.ipResultsMatching(
    ip -> ip.riskLevel() == RiskLevel.VERY_HIGH);
 
List<IpResult> fromUS = response.ipResultsMatching(
    ip -> "US".equals(ip.countryCode()));
```

#### Streaming

```java
// Stream all results with pattern matching
response.streamResults().forEach(result -> {
    switch (result) {
        case IpResult ip       -> processIp(ip);
        case EmailResult email -> processEmail(email);
    }
});
 
// Stream only IPs
response.streamIpResults()
    .filter(IpResult::isThreat)
    .forEach(ip -> blockAddress(ip.address()));
 
// Stream only emails
response.streamEmailResults()
    .filter(EmailResult::isDisposable)
    .map(EmailResult::address)
    .forEach(this::rejectRegistration);
```
 
---

### IP Result Details

Each `IpResult` aggregates all data sections the API may return:

```java
var ip = response.firstIpResult().orElseThrow();
 
// ── Convenience Accessors ──────────────────────────────────────
ip.address();               // "8.8.8.8"
ip.isThreat();              // true if proxy, VPN, TOR, compromised, or scraper
ip.riskLevel();             // LOW | MEDIUM | HIGH | VERY_HIGH
ip.accessRecommendation();  // ALLOW | CHALLENGE | DENY
ip.countryCode();           // "US"
ip.provider();              // "Google LLC"
ip.networkType();           // RESIDENTIAL | BUSINESS | WIRELESS | HOSTING
 
// ── Network ────────────────────────────────────────────────────
ip.network().asn();          // "AS15169"
ip.network().range();        // "8.8.8.0/24"
ip.network().hostname();     // "dns.google"
ip.network().provider();     // "Google LLC"
ip.network().organisation(); // "Google LLC"
ip.network().type();         // "Business"
 
// ── Location ───────────────────────────────────────────────────
ip.location().continentName(); // "North America"
ip.location().countryName();   // "United States"
ip.location().countryCode();   // "US"
ip.location().regionName();    // "California"
ip.location().cityName();      // "Mountain View"
ip.location().latitude();      // "37.386"
ip.location().longitude();     // "-122.084"
ip.location().timezone();      // "America/Los_Angeles"
ip.location().currency();      // Currency[code=USD, name=Dollar, symbol=$]
 
// ── Detections ─────────────────────────────────────────────────
ip.detections().isProxy();       // false
ip.detections().isVpn();         // false
ip.detections().isTor();         // false
ip.detections().isCompromised(); // false
ip.detections().isScraper();     // false
ip.detections().isHosting();     // true
ip.detections().isAnonymous();   // false
ip.detections().isThreat();      // aggregated: proxy|vpn|tor|compromised|scraper
ip.detections().risk();          // 0-100
ip.detections().confidence();    // 0-100
ip.detections().firstSeen();     // ISO 8601 timestamp or null
ip.detections().lastSeen();      // ISO 8601 timestamp or null
 
// ── Device Estimate ────────────────────────────────────────────
ip.deviceEstimate().address();   // devices behind this IP
ip.deviceEstimate().subnet();    // devices in the subnet
 
// ── Detection History ──────────────────────────────────────────
ip.detectionHistory().delisted();       // true = already delisted
ip.detectionHistory().delistDatetime(); // ISO 8601 timestamp
 
// ── Attack History ─────────────────────────────────────────────
ip.attackHistory().attacks();       // Map<String, Integer>
ip.attackHistory().totalAttacks();  // sum of all attack counts
 
// ── Operator (VPN/proxy provider info) ─────────────────────────
ip.operator().name();         // "Cloudflare WARP"
ip.operator().url();          // "https://..."
ip.operator().anonymity();    // "Low"
ip.operator().popularity();   // "Very High"
ip.operator().services();     // ["VPN"]
ip.operator().protocols();    // ["WireGuard"]
ip.operator().policies();     // OperatorPolicies record
```

### Email Result Details

```java
var email = response.firstEmailResult().orElseThrow();
 
email.address();       // "user@tempmail.org"
email.isDisposable();  // true
```
 
---

### Risk Assessment

#### Risk Levels

| Level | Score | Interpretation |
|-------|-------|----------------|
| `LOW` | 0 &ndash; 25 | Minimal risk, typically safe |
| `MEDIUM` | 26 &ndash; 50 | Moderate risk, warrants monitoring |
| `HIGH` | 51 &ndash; 75 | Elevated risk, likely suspicious |
| `VERY_HIGH` | 76 &ndash; 100 | Severe risk, strongly associated with abuse |

```java
RiskLevel level = RiskLevel.fromScore(66); // HIGH
```

#### Access Recommendations

Combines the risk score with the anonymity flag to produce an access decision:

| Risk Level | Anonymous | Not Anonymous |
|------------|-----------|---------------|
| LOW | CHALLENGE | ALLOW |
| MEDIUM | CHALLENGE | CHALLENGE |
| HIGH | DENY | CHALLENGE |
| VERY_HIGH | DENY | DENY |

```java
AccessRecommendation rec = ip.accessRecommendation();
 
// Or evaluate manually
AccessRecommendation.evaluate(66, true);  // DENY
AccessRecommendation.evaluate(10, false); // ALLOW
```
 
---

### Caching

The built-in LRU cache stores successful responses keyed by address + query flags:

```java
var client = ProxyCheckClient.builder()
    .apiKey("key")
    .cache(Duration.ofMinutes(10), 5000) // TTL, max entries
    .build();
 
client.check("8.8.8.8"); // API call
client.check("8.8.8.8"); // cache hit, no API call
 
client.invalidateCache("8.8.8.8"); // evict one entry
client.clearCache();                // evict all entries
```

**How it works:**
- Only successful responses (`OK` or `WARNING`) are cached
- Different query flags produce separate cache entries
- Expired entries are lazily evicted on access
- When full, the least-recently-used entry is evicted
- Batch results are cached individually per address for single-lookup hits

---

### Rate Limiting

Protect against API throttling (proxycheck.io warns at 175 req/s, denies at 200 req/s):

```java
var client = ProxyCheckClient.builder()
    .apiKey("key")
    .rateLimitPerSecond(150) // stay safely below limits
    .build();
```

| Call Type | Behavior When Exhausted |
|-----------|------------------------|
| Synchronous | Blocks until the next window refill |
| Asynchronous | Fails immediately with `ProxyCheckException` |
 
---

### Retry Policy

Automatic retry with exponential backoff for transient failures:

```java
// Default: 3 retries at 500ms -> 1s -> 2s (capped at 8s)
var client = ProxyCheckClient.of("key");
 
// Custom
var client = ProxyCheckClient.builder()
    .apiKey("key")
    .retryPolicy(RetryPolicy.builder()
        .maxRetries(5)
        .initialDelay(Duration.ofSeconds(1))
        .multiplier(3.0)
        .maxDelay(Duration.ofSeconds(30))
        .build())
    .build();
 
// Disable
var client = ProxyCheckClient.builder()
    .apiKey("key")
    .noRetry()
    .build();
```

**Retried:** `IOException` (network failures), HTTP 5xx (server errors)
**Not retried:** HTTP 4xx (client errors), parse failures, API rate limit denials
 
---

### Whitelist

Skip the API entirely for trusted addresses:

```java
var client = ProxyCheckClient.builder()
    .apiKey("key")
    .whitelist("127.0.0.1", "::1")
    .whitelist(List.of("10.0.0.1", "10.0.0.2"))
    .build();
 
// Returns ProxyCheckResponse.empty() instantly
client.check("127.0.0.1");
```

Whitelisted addresses are also filtered out of batch requests before the API call is made.
 
---

### Event Listeners

Monitor every stage of the request lifecycle:

```java
// Lambda-based builder
var listener = CheckListener.builder()
    .onRequest(addrs -> log.info("Checking {} addresses", addrs.size()))
    .onResponse((addrs, resp) -> metrics.record("query_time", resp.queryTime()))
    .onError((addrs, err) -> alerting.fire("proxycheck_error", err))
    .onCacheHit(addr -> metrics.increment("cache.hits"))
    .onRetry((attempt, cause) -> log.warn("Retry #{}: {}", attempt, cause.getMessage()))
    .build();
 
// Register via builder or at runtime
client.addListener(listener);
client.removeListener(listener);
```

Or implement the interface directly (all methods are default no-ops):

```java
public class MetricsListener implements CheckListener {
    @Override
    public void onRequest(Collection<String> addresses) {
        metrics.increment("api.requests");
    }
 
    @Override
    public void onResponse(Collection<String> addresses, ProxyCheckResponse response) {
        metrics.record("api.query_time", response.queryTime());
    }
}
```
 
---

### Address Validation

Client-side format validation for fast feedback before hitting the API:

```java
Addresses.isValidIpv4("192.168.1.1");     // true
Addresses.isValidIpv6("::1");              // true
Addresses.isValidIp("8.8.8.8");           // true (IPv4 or IPv6)
Addresses.isValidEmail("user@test.com");   // true
Addresses.isValid("8.8.8.8");             // true (any format)
 
// Throws IllegalArgumentException if invalid
Addresses.requireValid("not-an-address");
```
 
---

### Error Handling

All errors surface as the unchecked `ProxyCheckException`:

```java
try {
    var response = client.check("8.8.8.8");
} catch (ProxyCheckException e) {
    System.err.println("Message: " + e.getMessage());
 
    if (e.hasHttpStatusCode()) {
        System.err.println("HTTP status: " + e.httpStatusCode());
    }
 
    // Original cause (IOException, InterruptedException, etc.)
    System.err.println("Cause: " + e.getCause());
}
```
 
---

### Java Module System

This library ships with a `module-info.java` for JPMS-based applications:

```java
module your.app {
    requires io.proxycheck.api;
}
```

**Exported packages:**

| Package | Contents |
|---------|----------|
| `io.proxycheck.api` | `ProxyCheckClient`, `QueryFlags`, `RetryPolicy`, `CheckListener`, `Addresses` |
| `io.proxycheck.api.model` | `ProxyCheckResponse`, `IpResult`, `EmailResult`, `Result`, enums |
| `io.proxycheck.api.exception` | `ProxyCheckException` |
 
---

## Complete Example

```java
import io.proxycheck.api.*;
import io.proxycheck.api.model.*;
import java.time.Duration;
import java.util.List;
 
public class Example {
    public static void main(String[] args) {
        try (var client = ProxyCheckClient.builder()
                .apiKey("your-api-key")
                .cache(Duration.ofMinutes(5))
                .rateLimitPerSecond(150)
                .whitelist("127.0.0.1", "::1")
                .listener(CheckListener.builder()
                    .onError((addrs, err) ->
                        System.err.println("Error: " + err.getMessage()))
                    .build())
                .build()) {
 
            // Check mixed IPs and emails in one batch
            var response = client.check(
                List.of("8.8.8.8", "1.1.1.1", "user@tempmail.org"),
                QueryFlags.detailed()
            );
 
            if (!response.isSuccessful()) {
                System.err.println("API error: " + response.message());
                return;
            }
 
            // Block threats
            response.threatIps().forEach(ip ->
                System.out.printf("BLOCKED %s  risk=%d  country=%s  provider=%s%n",
                    ip.address(), ip.detections().risk(),
                    ip.countryCode(), ip.provider()));
 
            // Reject disposable emails
            response.disposableEmails().forEach(email ->
                System.out.printf("REJECTED %s (disposable)%n", email.address()));
 
            // Pattern matching on all results
            response.streamResults().forEach(result -> {
                switch (result) {
                    case IpResult ip -> System.out.printf(
                        "IP %-15s  risk=%-9s  action=%s%n",
                        ip.address(), ip.riskLevel(), ip.accessRecommendation());
                    case EmailResult email -> System.out.printf(
                        "Email %-30s  disposable=%s%n",
                        email.address(), email.isDisposable());
                }
            });
        }
    }
}
```

## Architecture

```
io.proxycheck.api
├── ProxyCheckClient       Main client: sync/async checks, caching, retries
├── ProxyCheckClient.Builder  Fluent builder for client configuration
├── QueryFlags             Query parameter builder (days, node, short, tag, etc.)
├── RetryPolicy            Exponential backoff configuration
├── CheckListener          Observer interface for lifecycle events
├── ResponseCache          TTL + LRU eviction cache (package-private)
├── RateLimiter            Token-bucket rate limiter (package-private)
├── ResponseParser         JSON-to-model deserialization (package-private)
├── Addresses              IPv4/IPv6/email validation utilities
└── Example                Comprehensive usage examples
 
io.proxycheck.api.model
├── ProxyCheckResponse     Top-level response with filtering and streaming
├── Result (sealed)        Base type for pattern matching
│   ├── IpResult           Full IP check result (record)
│   └── EmailResult        Email check result (record)
├── Network                ASN, range, provider, type
├── Location               Geo data: continent → city + coordinates
├── Currency               ISO 4217 currency from location
├── DeviceEstimate         Device count behind IP/subnet
├── Detections             Threat flags + risk/confidence scores
├── DetectionHistory       Listing/delisting status
├── AttackHistory          Attack type → count map
├── Operator               VPN/proxy provider metadata
├── OperatorPolicies       Provider policy flags
├── NetworkType (enum)     RESIDENTIAL | BUSINESS | WIRELESS | HOSTING
├── RiskLevel (enum)       LOW | MEDIUM | HIGH | VERY_HIGH
├── AccessRecommendation   ALLOW | CHALLENGE | DENY
├── ResponseStatus (enum)  OK | WARNING | DENIED | ERROR
└── StatusMessage (enum)   Known API warning/error messages
 
io.proxycheck.api.exception
└── ProxyCheckException    Unchecked exception with optional HTTP status code
```

## Running Tests

```bash
./gradlew test
```

The test suite contains **164 tests** covering all client components, model parsing, caching behavior, rate limiting, retry logic, and edge cases.

## Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| [Gson](https://github.com/google/gson) | 2.13.2 | JSON deserialization |
| [JUnit 5](https://junit.org/junit5/) | 5.14.1 | Testing (test scope only) |

No transitive dependencies. No frameworks. No annotation processors.

## Contributing

Contributions are welcome! Here's how to get started:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/my-feature`)
3. **Write** tests for your changes
4. **Ensure** all tests pass (`./gradlew test`)
5. **Commit** with a clear message (`git commit -m "Add support for ..."`)
6. **Push** to your branch (`git push origin feature/my-feature`)
7. **Open** a Pull Request

### Guidelines

- Follow existing code style and naming conventions
- Maintain backward compatibility for public APIs
- Add Javadoc for all new public methods and classes
- Keep the single-dependency philosophy &mdash; avoid adding new runtime dependencies
- Target Java 21+ features (records, sealed types, pattern matching)

## License

This project is licensed under the **GNU General Public License v3.0** &mdash; see the [LICENSE](LICENSE) file for details.
 
---

<p align="center">
  Built with Java 21 &bull; Powered by <a href="https://proxycheck.io">proxycheck.io</a>
</p>
