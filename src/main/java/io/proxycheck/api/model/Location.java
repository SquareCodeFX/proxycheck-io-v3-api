package io.proxycheck.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Geolocation data for an IP address, spanning continent through city level.
 * Includes coordinates, timezone, and the local currency of the resolved country.
 * All fields may be {@code null} if the API could not resolve the location.
 */
public record Location(
        @SerializedName("continent_name") String continentName,
        @SerializedName("continent_code") String continentCode,
        @SerializedName("country_name") String countryName,
        @SerializedName("country_code") String countryCode,
        @SerializedName("region_name") String regionName,
        @SerializedName("region_code") String regionCode,
        @SerializedName("city_name") String cityName,
        @SerializedName("postal_code") String postalCode,
        String latitude,
        String longitude,
        String timezone,
        Currency currency
) {}
