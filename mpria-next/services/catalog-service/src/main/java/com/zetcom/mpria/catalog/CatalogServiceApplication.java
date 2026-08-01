package com.zetcom.mpria.catalog;

import java.time.Clock;

import com.zetcom.mpria.catalog.infrastructure.configclient.ConfigServiceProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Catalogue service — the transactional write core.
 *
 * <p>Owns {@code catalog_record}. No other service writes it (ARCHITECTURE.md §5).
 */
@SpringBootApplication
@EnableConfigurationProperties(ConfigServiceProperties.class)
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }

    /**
     * Time is a dependency, not an ambient fact. Injecting a {@link Clock} is what lets the domain
     * tests assert on timestamps instead of tolerating them.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
