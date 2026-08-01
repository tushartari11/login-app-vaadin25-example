package com.zetcom.mpria.catalog.infrastructure.configclient;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the configuration service lives, and how patient this service is with it.
 *
 * @param baseUrl root URL of config-service
 * @param timeout per-request timeout; short by design, because a cached schema is always available
 */
@ConfigurationProperties(prefix = "mpria.config-service")
public record ConfigServiceProperties(String baseUrl, Duration timeout) {

    public ConfigServiceProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("mpria.config-service.base-url is required");
        }
        timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
    }
}
