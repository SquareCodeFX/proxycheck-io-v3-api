package io.proxycheck.api.model;

import java.util.Map;

/**
 * A single tag entry exported from the dashboard, containing a breakdown of
 * detection types and optionally the IP addresses associated with the tag.
 *
 * @param types     detection type counts (total, proxy, vpn, rule, blacklist)
 * @param addresses map of IP address to check count, present when {@code addresses=1}
 *                  was supplied in the request; {@code null} otherwise
 */
public record TagEntry(
        TagTypes types,
        Map<String, Integer> addresses
) {}
