package com.zetcom.mpria.config.infrastructure.memory;

import com.zetcom.mpria.config.application.port.ConfigurationRepository;
import com.zetcom.mpria.config.infrastructure.seed.SeedRevisionReader;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the configuration store to its seed.
 *
 * <p>Kept out of the application class deliberately: a {@code @Bean} method declared there is
 * created by every web slice test as well, and a controller slice has no business reading a
 * classpath resource to answer a mocked call.
 */
@Configuration
class ConfigurationStoreConfiguration {

    /** A malformed seed fails startup rather than leaving the service with nothing to serve. */
    @Bean
    ConfigurationRepository configurationRepository(SeedRevisionReader seed) {
        return new InMemoryConfigurationRepository(seed.read());
    }
}
