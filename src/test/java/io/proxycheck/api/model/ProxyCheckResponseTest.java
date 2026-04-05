package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProxyCheckResponseTest {

    private static IpResult safeIp(String addr) {
        var detections = new Detections(false, false, false, false, false, false, false, 5, 100, null, null);
        return new IpResult(addr, null, null, null, detections, null, null, null, null);
    }

    private static IpResult threatIp(String addr) {
        var detections = new Detections(true, false, false, false, false, false, true, 80, 100, null, null);
        return new IpResult(addr, null, null, null, detections, null, null, null, null);
    }

    @Test
    void ipResultLookup() {
        var ip = new IpResult("1.2.3.4", null, null, null, null, null, null, null, null);
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addIpResult("1.2.3.4", ip)
                .build();

        assertTrue(response.ipResult("1.2.3.4").isPresent());
        assertEquals(ip, response.ipResult("1.2.3.4").get());
        assertTrue(response.ipResult("5.6.7.8").isEmpty());
    }

    @Test
    void emailResultLookup() {
        var email = new EmailResult("a@b.com", false);
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addEmailResult("a@b.com", email)
                .build();

        assertTrue(response.emailResult("a@b.com").isPresent());
        assertEquals(email, response.emailResult("a@b.com").get());
        assertTrue(response.emailResult("x@y.com").isEmpty());
    }

    @Test
    void totalResults() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addIpResult("1.2.3.4", safeIp("1.2.3.4"))
                .addIpResult("5.6.7.8", safeIp("5.6.7.8"))
                .addEmailResult("a@b.com", new EmailResult("a@b.com", false))
                .build();

        assertEquals(3, response.totalResults());
    }

    @Test
    void emptyResponse() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.ERROR)
                .message("No valid IP Addresses supplied.")
                .build();

        assertEquals(0, response.totalResults());
        assertTrue(response.firstIpResult().isEmpty());
        assertTrue(response.firstEmailResult().isEmpty());
    }

    @Test
    void toStringHandlesNulls() {
        var response = new ProxyCheckResponse.Builder().build();
        assertNotNull(response.toString());
        assertDoesNotThrow(response::toString);
    }

    @Test
    void emptyFactory() {
        var empty = ProxyCheckResponse.empty();
        assertNotNull(empty);
        assertEquals(0, empty.totalResults());
        assertNull(empty.status());
        assertSame(empty, ProxyCheckResponse.empty());
    }

    @Test
    void threatIpsFilter() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addIpResult("1.1.1.1", safeIp("1.1.1.1"))
                .addIpResult("2.2.2.2", threatIp("2.2.2.2"))
                .addIpResult("3.3.3.3", threatIp("3.3.3.3"))
                .build();

        assertEquals(2, response.threatIps().size());
        assertEquals(1, response.safeIps().size());
        assertTrue(response.hasThreat());
    }

    @Test
    void noThreats() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addIpResult("1.1.1.1", safeIp("1.1.1.1"))
                .build();

        assertTrue(response.threatIps().isEmpty());
        assertFalse(response.hasThreat());
    }

    @Test
    void disposableEmailFilter() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addEmailResult("good@gmail.com", new EmailResult("good@gmail.com", false))
                .addEmailResult("bad@temp.org", new EmailResult("bad@temp.org", true))
                .build();

        assertEquals(1, response.disposableEmails().size());
        assertEquals(1, response.legitimateEmails().size());
        assertTrue(response.hasDisposableEmail());
    }

    @Test
    void noDisposableEmails() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addEmailResult("good@gmail.com", new EmailResult("good@gmail.com", false))
                .build();

        assertTrue(response.disposableEmails().isEmpty());
        assertFalse(response.hasDisposableEmail());
    }

    @Test
    void streamIpResults() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addIpResult("1.1.1.1", safeIp("1.1.1.1"))
                .addIpResult("2.2.2.2", threatIp("2.2.2.2"))
                .build();

        assertEquals(2, response.streamIpResults().count());
        assertEquals(1, response.streamIpResults().filter(IpResult::isThreat).count());
    }

    @Test
    void streamEmailResults() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addEmailResult("a@b.com", new EmailResult("a@b.com", true))
                .build();

        assertEquals(1, response.streamEmailResults().count());
    }

    @Test
    void sealedResultLookup() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addIpResult("1.1.1.1", safeIp("1.1.1.1"))
                .addEmailResult("a@b.com", new EmailResult("a@b.com", false))
                .build();

        var ipResult = response.result("1.1.1.1");
        assertTrue(ipResult.isPresent());
        assertInstanceOf(IpResult.class, ipResult.get());

        var emailResult = response.result("a@b.com");
        assertTrue(emailResult.isPresent());
        assertInstanceOf(EmailResult.class, emailResult.get());

        assertTrue(response.result("unknown").isEmpty());
    }

    @Test
    void sealedResultPatternMatching() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addIpResult("1.1.1.1", threatIp("1.1.1.1"))
                .addEmailResult("a@b.com", new EmailResult("a@b.com", true))
                .build();

        response.streamResults().forEach(result -> {
            switch (result) {
                case IpResult ip -> assertNotNull(ip.detections());
                case EmailResult email -> assertTrue(email.isDisposable());
            }
        });
    }

    @Test
    void streamResultsCombinesBothTypes() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addIpResult("1.1.1.1", safeIp("1.1.1.1"))
                .addEmailResult("a@b.com", new EmailResult("a@b.com", false))
                .build();

        assertEquals(2, response.streamResults().count());
    }

    @Test
    void ipResultsMatchingPredicate() {
        var response = new ProxyCheckResponse.Builder()
                .status(ResponseStatus.OK)
                .addIpResult("1.1.1.1", safeIp("1.1.1.1"))
                .addIpResult("2.2.2.2", threatIp("2.2.2.2"))
                .addIpResult("3.3.3.3", threatIp("3.3.3.3"))
                .build();

        var highRisk = response.ipResultsMatching(ip ->
                ip.riskLevel() == RiskLevel.VERY_HIGH);
        assertEquals(2, highRisk.size());

        var lowRisk = response.ipResultsMatching(ip ->
                ip.riskLevel() == RiskLevel.LOW);
        assertEquals(1, lowRisk.size());
    }
}
