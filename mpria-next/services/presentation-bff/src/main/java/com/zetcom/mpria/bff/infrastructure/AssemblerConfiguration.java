package com.zetcom.mpria.bff.infrastructure;

import com.zetcom.mpria.bff.application.PresentationModelAssembler;
import com.zetcom.mpria.bff.application.port.CatalogGateway;
import com.zetcom.mpria.bff.application.port.ConfigurationGateway;
import com.zetcom.mpria.bff.infrastructure.catalogclient.CatalogServiceProperties;
import com.zetcom.mpria.bff.infrastructure.configclient.ConfigServiceProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring's knowledge of the application layer, kept in one place.
 *
 * <p>Deliberately <em>not</em> on the {@code @SpringBootApplication} class. A slice test filters out
 * the {@code @Component} adapters but still processes {@code @Bean} methods on the application class,
 * so a factory method needing a gateway would fail every {@code @WebMvcTest} with an unsatisfied
 * dependency. A plain {@code @Configuration} is excluded from the slice wholesale, which is what makes
 * the controller testable on its own.
 *
 * <p>This is also why {@link PresentationModelAssembler} is wired rather than annotated: it carries no
 * Spring annotation at all, so its own tests construct it with two fakes and run in microseconds.
 */
@Configuration
@EnableConfigurationProperties({ConfigServiceProperties.class, CatalogServiceProperties.class})
class AssemblerConfiguration {

    @Bean
    PresentationModelAssembler presentationModelAssembler(ConfigurationGateway configuration,
                                                          CatalogGateway catalog) {
        return new PresentationModelAssembler(configuration, catalog);
    }
}
