package com.zetcom.mpria.catalog;

import com.zetcom.mpria.catalog.application.CatalogRecordService;
import com.zetcom.mpria.catalog.application.port.RecordRepository;
import com.zetcom.mpria.catalog.application.port.RecordSchemaProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the application actually starts.
 *
 * <p>Every other test in this module is a slice: unit tests touch no context, {@code @DataJpaTest}
 * loads persistence only, and {@code @WebMvcTest} loads the web layer with the use cases mocked.
 * None of them instantiates {@link com.zetcom.mpria.catalog.infrastructure.configclient
 * ConfigServiceSchemaProvider}, so a missing dependency of that adapter could sit behind a
 * completely green build and only surface on the first real startup.
 *
 * <p>That is not hypothetical — it is exactly what happened here. The adapter injects a
 * {@code RestClient.Builder}, whose autoconfiguration Spring Boot 4 moved out of
 * {@code spring-boot-starter-web} into {@code spring-boot-starter-restclient}. The module built
 * green while being unable to start. This test is the gap that let that through.
 */
@SpringBootTest
@Testcontainers
class CatalogServiceApplicationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    /** Avoids reaching a real OIDC issuer to build the resource server's decoder at startup. */
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    CatalogRecordService catalogRecordService;

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    RecordSchemaProvider recordSchemaProvider;

    @Test
    void the_context_starts_with_every_port_bound_to_a_real_adapter() {
        assertThat(catalogRecordService).isNotNull();
        assertThat(recordRepository).isNotNull();
        // The port that needs RestClient.Builder. Asserting it is wired is the whole point.
        assertThat(recordSchemaProvider).isNotNull();
    }
}
