package io.proxycheck.api;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DashboardClientTest {

    @Test
    void builderRequiresApiKey() {
        assertThrows(NullPointerException.class, () -> DashboardClient.builder().build());
    }

    @Test
    void builderCreatesClient() {
        try (var client = DashboardClient.builder().apiKey("test-key").noRetry().build()) {
            assertNotNull(client);
        }
    }

    @Test
    void builderWithTimeout() {
        try (var client = DashboardClient.builder()
                .apiKey("test-key")
                .timeout(Duration.ofSeconds(10))
                .noRetry()
                .build()) {
            assertNotNull(client);
        }
    }

    @Test
    void builderWithRetryPolicy() {
        try (var client = DashboardClient.builder()
                .apiKey("test-key")
                .retryPolicy(RetryPolicy.defaultPolicy())
                .build()) {
            assertNotNull(client);
        }
    }

    @Test
    void proxyCheckClientProvidesDashboardClient() {
        try (var proxyClient = ProxyCheckClient.of("test-key")) {
            DashboardClient dash = proxyClient.dashboard();
            assertNotNull(dash);
            // closing the dashboard client obtained from ProxyCheckClient is safe and has no effect
            dash.close();
            // ProxyCheckClient is still usable after closing the shared DashboardClient
            assertNotNull(proxyClient.dashboard());
        }
    }

    @Test
    void apiKeyNullRejectedByBuilder() {
        assertThrows(NullPointerException.class,
                () -> DashboardClient.builder().apiKey(null));
    }

    @Test
    void timeoutNullRejectedByBuilder() {
        assertThrows(NullPointerException.class,
                () -> DashboardClient.builder().apiKey("key").timeout(null));
    }

    @Test
    void retryPolicyNullRejectedByBuilder() {
        assertThrows(NullPointerException.class,
                () -> DashboardClient.builder().apiKey("key").retryPolicy(null));
    }

    // -------------------------------------------------------------------------
    // Null-safety for public API methods
    // -------------------------------------------------------------------------

    @Test
    void printListNullNameThrows() {
        try (var client = DashboardClient.builder().apiKey("key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.printList(null));
        }
    }

    @Test
    void printListWithTypeNullNameThrows() {
        try (var client = DashboardClient.builder().apiKey("key").noRetry().build()) {
            assertThrows(NullPointerException.class,
                    () -> client.printList(null, io.proxycheck.api.model.ListType.WHITELIST));
        }
    }

    @Test
    void printListWithTypeNullTypeThrows() {
        try (var client = DashboardClient.builder().apiKey("key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.printList("mylist", null));
        }
    }

    @Test
    void printRuleNullIdThrows() {
        try (var client = DashboardClient.builder().apiKey("key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.printRule(null));
        }
    }

    @Test
    void printRuleByNameNullThrows() {
        try (var client = DashboardClient.builder().apiKey("key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.printRuleByName(null));
        }
    }

    @Test
    void enableRuleNullIdThrows() {
        try (var client = DashboardClient.builder().apiKey("key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.enableRule(null));
        }
    }

    @Test
    void disableRuleByNameNullThrows() {
        try (var client = DashboardClient.builder().apiKey("key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.disableRuleByName(null));
        }
    }

    @Test
    void addOriginsVarargsNullThrows() {
        try (var client = DashboardClient.builder().apiKey("key").noRetry().build()) {
            assertThrows(NullPointerException.class,
                    () -> client.addOrigins((String[]) null));
        }
    }

    @Test
    void clearListNullNameThrows() {
        try (var client = DashboardClient.builder().apiKey("key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.clearList(null));
        }
    }

    @Test
    void eraseListNullNameThrows() {
        try (var client = DashboardClient.builder().apiKey("key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.eraseList(null));
        }
    }

    @Test
    void forceDownloadListNullNameThrows() {
        try (var client = DashboardClient.builder().apiKey("key").noRetry().build()) {
            assertThrows(NullPointerException.class, () -> client.forceDownloadList(null));
        }
    }
}
