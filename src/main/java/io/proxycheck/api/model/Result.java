package io.proxycheck.api.model;

/**
 * Sealed base type for all proxycheck.io results. Enables exhaustive pattern matching:
 * <pre>{@code
 * switch (result) {
 *     case IpResult ip   -> handleIp(ip);
 *     case EmailResult e -> handleEmail(e);
 * }
 * }</pre>
 */
public sealed interface Result permits IpResult, EmailResult {

    String address();
}
