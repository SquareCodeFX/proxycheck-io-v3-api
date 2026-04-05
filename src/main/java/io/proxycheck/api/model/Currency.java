package io.proxycheck.api.model;

/**
 * Currency associated with a geolocation result.
 *
 * @param code   ISO 4217 currency code (e.g., "USD", "EUR")
 * @param name   human-readable currency name (e.g., "United States Dollar")
 * @param symbol the currency symbol (e.g., "$", "EUR")
 */
public record Currency(
        String code,
        String name,
        String symbol
) {}
