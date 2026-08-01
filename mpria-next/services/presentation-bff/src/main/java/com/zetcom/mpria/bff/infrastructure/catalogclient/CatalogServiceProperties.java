package com.zetcom.mpria.bff.infrastructure.catalogclient;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the catalogue service lives.
 *
 * <p>Only the address is a per-upstream concern. Connect and read timeouts are set once for every
 * HTTP client this service makes, under {@code spring.http.clients}.
 */
@ConfigurationProperties(prefix = "mpria.catalog-service")
public record CatalogServiceProperties(String baseUrl) {

    public CatalogServiceProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("mpria.catalog-service.base-url is required");
        }
    }
}
