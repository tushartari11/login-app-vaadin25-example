package com.zetcom.mpria.config;

import java.time.Clock;

import com.zetcom.mpria.config.domain.ComponentRegistry;
import com.zetcom.mpria.config.domain.ConfigurationValidator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * The administration application: record types, field definitions, screen layouts and immutable
 * configuration revisions.
 *
 * <p>It starts and is fully usable with every other service down, and the reverse holds too —
 * consumers pin to a published revision and cache it, so this service is not on the runtime
 * critical path and may be restarted, upgraded or firewalled to an internal network without
 * affecting cataloguing (docs/ui-configuration.md §2).
 */
@SpringBootApplication
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }

    /**
     * Time is a dependency, not an ambient fact. Injecting a {@link Clock} is what lets the tests
     * assert on a publication timestamp instead of tolerating it.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ComponentRegistry componentRegistry() {
        return ComponentRegistry.standard();
    }

    @Bean
    ConfigurationValidator configurationValidator(ComponentRegistry registry) {
        return new ConfigurationValidator(registry);
    }
}
