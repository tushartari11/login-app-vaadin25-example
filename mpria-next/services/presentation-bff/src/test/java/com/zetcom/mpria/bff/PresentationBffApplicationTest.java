package com.zetcom.mpria.bff;

import com.zetcom.mpria.bff.application.PresentationModelAssembler;
import com.zetcom.mpria.bff.application.port.CatalogGateway;
import com.zetcom.mpria.bff.application.port.ConfigurationGateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The wiring, as the process will actually build it.
 *
 * <p>Slice tests prove behaviour but not assembly: every other test here either constructs the
 * assembler by hand or runs a {@code @WebMvcTest} that never creates an outbound adapter. This one
 * refreshes the real context, so a missing bean — an unbound property, an adapter asking for a
 * {@code RestClient.Builder} no auto-configuration provides — fails the build instead of the first
 * deployment.
 *
 * <p>{@code JwtDecoder} is mocked because the real one is built from the realm's issuer URI and
 * would fetch its OIDC metadata over the network while the context starts.
 */
@SpringBootTest
class PresentationBffApplicationTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private PresentationModelAssembler assembler;

    @Autowired
    private ConfigurationGateway configuration;

    @Autowired
    private CatalogGateway catalog;

    @Test
    void the_context_starts_with_both_gateways_and_the_assembler_wired() {
        assertThat(assembler).isNotNull();
        assertThat(configuration).isNotNull();
        assertThat(catalog).isNotNull();
    }
}
