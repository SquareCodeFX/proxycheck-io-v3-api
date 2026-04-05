package io.proxycheck.api;


/**
 * <p>
 * Class: Main
 * </p>
 * <p>
 * Author: squarecode
 * </p>
 * <p>
 * Date: 05.04.2026
 * </p>
 */
public class Main {

    public static void main(String[] args) {
        ProxyCheckClient.of("").check("paxoge2268@cosdas.com").firstEmailResult().ifPresent(System.out::println);
    }
}
