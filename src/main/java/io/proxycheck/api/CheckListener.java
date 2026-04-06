package io.proxycheck.api;

import io.proxycheck.api.model.ProxyCheckResponse;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Observer for client lifecycle events. All methods have no-op defaults, so
 * implementations only need to override the events they care about.
 *
 * <p>Use {@link #builder()} for a lambda-based listener, or implement this
 * interface directly for a class-based approach.
 */
public interface CheckListener {

    default void onRequest(Collection<String> addresses) {}

    default void onResponse(Collection<String> addresses, ProxyCheckResponse response) {}

    default void onError(Collection<String> addresses, Exception error) {}

    default void onCacheHit(String address) {}

    default void onRetry(int attempt, Exception cause) {}

    static Builder builder() {
        return new Builder();
    }

    final class Builder {
        private Consumer<Collection<String>> onRequest;
        private BiConsumer<Collection<String>, ProxyCheckResponse> onResponse;
        private BiConsumer<Collection<String>, Exception> onError;
        private Consumer<String> onCacheHit;
        private BiConsumer<Integer, Exception> onRetry;

        private Builder() {}

        public Builder onRequest(Consumer<Collection<String>> handler) {
            this.onRequest = handler;
            return this;
        }

        public Builder onResponse(BiConsumer<Collection<String>, ProxyCheckResponse> handler) {
            this.onResponse = handler;
            return this;
        }

        public Builder onError(BiConsumer<Collection<String>, Exception> handler) {
            this.onError = handler;
            return this;
        }

        public Builder onCacheHit(Consumer<String> handler) {
            this.onCacheHit = handler;
            return this;
        }

        public Builder onRetry(BiConsumer<Integer, Exception> handler) {
            this.onRetry = handler;
            return this;
        }

        // Builder.this.xxx is required to disambiguate the field from the
        // overridden method parameter with the same name in the anonymous class
        public CheckListener build() {
            return new CheckListener() {
                @Override
                public void onRequest(Collection<String> addresses) {
                    if (onRequest != null) onRequest.accept(addresses);
                }
                @Override
                public void onResponse(Collection<String> addresses, ProxyCheckResponse response) {
                    if (Builder.this.onResponse != null) Builder.this.onResponse.accept(addresses, response);
                }
                @Override
                public void onError(Collection<String> addresses, Exception error) {
                    if (Builder.this.onError != null) Builder.this.onError.accept(addresses, error);
                }
                @Override
                public void onCacheHit(String address) {
                    if (Builder.this.onCacheHit != null) Builder.this.onCacheHit.accept(address);
                }
                @Override
                public void onRetry(int attempt, Exception cause) {
                    if (Builder.this.onRetry != null) Builder.this.onRetry.accept(attempt, cause);
                }
            };
        }
    }
}
