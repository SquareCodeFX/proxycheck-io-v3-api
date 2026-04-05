package io.proxycheck.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Account usage statistics for today, as returned by the dashboard usage API.
 * Stats are delayed by a few minutes on the server side.
 *
 * @param burstTokensAvailable number of burst tokens currently available
 * @param burstTokenAllowance  total burst token allowance for the account
 * @param queriesToday         number of queries made today (as a string)
 * @param dailyLimit           maximum queries allowed per day (as a string)
 * @param queriesTotal         all-time total query count (as a string)
 * @param planTier             the account plan name (e.g. "Free", "Starter")
 */
public record AccountUsage(
        @SerializedName("Burst Tokens Available") int burstTokensAvailable,
        @SerializedName("Burst Token Allowance") int burstTokenAllowance,
        @SerializedName("Queries Today") String queriesToday,
        @SerializedName("Daily Limit") String dailyLimit,
        @SerializedName("Queries Total") String queriesTotal,
        @SerializedName("Plan Tier") String planTier
) {}
