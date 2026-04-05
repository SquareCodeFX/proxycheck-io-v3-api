package io.proxycheck.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Policy information for an operator, describing the features and privacy
 * characteristics of their service.
 */
public record OperatorPolicies(
        @SerializedName("ad_filtering") Boolean adFiltering,
        @SerializedName("free_access") Boolean freeAccess,
        @SerializedName("paid_access") Boolean paidAccess,
        @SerializedName("port_forwarding") Boolean portForwarding,
        Boolean logging,
        @SerializedName("anonymous_payments") Boolean anonymousPayments,
        @SerializedName("crypto_payments") Boolean cryptoPayments,
        @SerializedName("traceable_ownership") Boolean traceableOwnership
) {}
