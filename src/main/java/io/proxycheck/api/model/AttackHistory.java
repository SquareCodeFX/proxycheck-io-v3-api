package io.proxycheck.api.model;

import java.util.Map;

/**
 * Attack history for an IP address. Contains a map of attack types to their
 * occurrence counts. Examples of attack types include {@code "login_attempt"}
 * and {@code "registration_attempt"}.
 *
 * <p>The API recommends adding together the counts for each attack type and
 * making a decision based on the total.
 */
public record AttackHistory(
        Map<String, Integer> attacks
) {

    /**
     * Returns the total count of all attack types combined.
     */
    public int totalAttacks() {
        if (attacks == null || attacks.isEmpty()) {
            return 0;
        }
        return attacks.values().stream().mapToInt(Integer::intValue).sum();
    }
}
