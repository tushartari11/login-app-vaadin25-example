package com.zetcom.mpria.bff.infrastructure.configclient;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the configuration service lives.
 *
 * <p>Only the address is a per-upstream concern. Connect and read timeouts are set once for every
 * HTTP client this service makes, under {@code spring.http.clients}, so two adapters cannot drift
 * into two different ideas of patience.
 */
@ConfigurationProperties(prefix = "mpria.config-service")
public record ConfigServiceProperties(String baseUrl) {

    public ConfigServiceProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("mpria.config-service.base-url is required");
        }
    }
}
