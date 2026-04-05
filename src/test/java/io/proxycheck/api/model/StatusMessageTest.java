package io.proxycheck.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusMessageTest {

    @Test
    void fromKnownWarningMessage() {
        var sm = StatusMessage.fromMessage("Your API Key has been disabled for a violation of our terms of service.");
        assertEquals(StatusMessage.API_KEY_DISABLED, sm);
        assertEquals(ResponseStatus.WARNING, sm.status());
    }

    @Test
    void fromKnownDeniedMessage() {
        var sm = StatusMessage.fromMessage(
                "Your access to the API has been blocked due to using a proxy server to perform your query. Please signup for an account to re-enable access by proxy.");
        assertEquals(StatusMessage.BLOCKED_BY_PROXY, sm);
        assertEquals(ResponseStatus.DENIED, sm.status());
    }

    @Test
    void fromKnownErrorMessage() {
        var sm = StatusMessage.fromMessage("No valid IP Addresses supplied.");
        assertEquals(StatusMessage.NO_VALID_IPS, sm);
        assertEquals(ResponseStatus.ERROR, sm.status());
    }

    @Test
    void rateLimitMessages() {
        assertEquals(StatusMessage.RATE_LIMIT_175,
                StatusMessage.fromMessage("You're sending more than 175 requests per second."));
        assertEquals(StatusMessage.RATE_LIMIT_200,
                StatusMessage.fromMessage("You're sending more than 200 requests per second."));
    }

    @Test
    void queryLimitMessages() {
        assertNotNull(StatusMessage.fromMessage("You are within 10% of your query limit for the day."));
        assertNotNull(StatusMessage.fromMessage(
                "1,000 Free queries exhausted. Please try the API again tomorrow or purchase a higher paid plan."));
        assertNotNull(StatusMessage.fromMessage(
                "100 queries exhausted, if you sign up for a free API key you'll be able to make 1,000 free queries per day."));
    }

    @Test
    void fromNullReturnsNull() {
        assertNull(StatusMessage.fromMessage(null));
    }

    @Test
    void fromUnknownMessageReturnsNull() {
        assertNull(StatusMessage.fromMessage("Some unknown message"));
    }
}
