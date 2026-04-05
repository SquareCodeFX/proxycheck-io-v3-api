package io.proxycheck.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Operator information for an IP address. Operators include VPN companies,
 * residential proxy companies, or webpage scraping companies.
 *
 * <p>If no operator information is available, the API returns {@code operator: null}
 * rather than omitting the section entirely (as of the 11-February-2026 release).
 */
public record Operator(
        String name,
        String url,
        String anonymity,
        String popularity,
        List<String> services,
        List<String> protocols,
        OperatorPolicies policies,
        @SerializedName("additional_operators") List<Operator> additionalOperators
) {}
