package io.proxycheck.api;

import io.proxycheck.api.model.ProxyCheckResponse;
import io.proxycheck.api.model.ResponseStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CheckListenerTest {

    @Test
    void fluentBuilderOnRequest() {
        var captured = new AtomicReference<String>();
        var listener = CheckListener.builder()
                .onRequest(addresses -> captured.set(addresses.iterator().next()))
                .build();

        listener.onRequest(List.of("1.2.3.4"));
        assertEquals("1.2.3.4", captured.get());
    }

    @Test
    void fluentBuilderOnResponse() {
        var captured = new AtomicReference<ProxyCheckResponse>();
        var response = new ProxyCheckResponse.Builder().status(ResponseStatus.OK).build();
        var listener = CheckListener.builder()
                .onResponse((addrs, resp) -> captured.set(resp))
                .build();

        listener.onResponse(List.of("1.2.3.4"), response);
        assertSame(response, captured.get());
    }

    @Test
    void fluentBuilderOnError() {
        var captured = new AtomicReference<Exception>();
        var error = new RuntimeException("test");
        var listener = CheckListener.builder()
                .onError((addrs, err) -> captured.set(err))
                .build();

        listener.onError(List.of("1.2.3.4"), error);
        assertSame(error, captured.get());
    }

    @Test
    void fluentBuilderOnCacheHit() {
        var captured = new AtomicReference<String>();
        var listener = CheckListener.builder()
                .onCacheHit(captured::set)
                .build();

        listener.onCacheHit("1.2.3.4");
        assertEquals("1.2.3.4", captured.get());
    }

    @Test
    void fluentBuilderOnRetry() {
        var captured = new AtomicReference<Integer>();
        var listener = CheckListener.builder()
                .onRetry((attempt, cause) -> captured.set(attempt))
                .build();

        listener.onRetry(3, new RuntimeException());
        assertEquals(3, captured.get());
    }

    @Test
    void unsetHandlersDoNotThrow() {
        var listener = CheckListener.builder().build();
        assertDoesNotThrow(() -> listener.onRequest(List.of()));
        assertDoesNotThrow(() -> listener.onResponse(List.of(), null));
        assertDoesNotThrow(() -> listener.onError(List.of(), null));
        assertDoesNotThrow(() -> listener.onCacheHit("x"));
        assertDoesNotThrow(() -> listener.onRetry(0, null));
    }

    @Test
    void defaultMethodsDoNotThrow() {
        CheckListener listener = new CheckListener() {};
        assertDoesNotThrow(() -> listener.onRequest(List.of()));
        assertDoesNotThrow(() -> listener.onCacheHit("x"));
    }
}
