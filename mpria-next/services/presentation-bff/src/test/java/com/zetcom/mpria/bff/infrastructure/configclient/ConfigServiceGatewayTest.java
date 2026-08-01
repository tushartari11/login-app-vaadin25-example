package com.zetcom.mpria.bff.infrastructure.configclient;

import java.util.Map;

import com.zetcom.mpria.bff.application.IncompleteCallerTokenException;
import com.zetcom.mpria.bff.application.UpstreamUnavailableException;
import com.zetcom.mpria.bff.domain.ConfigurationRevision;
import com.zetcom.mpria.bff.domain.RecordSchema;
import com.zetcom.mpria.bff.domain.ScreenDefinition;
import com.zetcom.mpria.bff.domain.ScreenNotFoundException;
import com.zetcom.mpria.bff.testing.CallerAuthentication;
import com.zetcom.mpria.bff.testing.ScreenFixtures;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static com.zetcom.mpria.bff.testing.ScreenFixtures.REVISION_ID;
import static com.zetcom.mpria.bff.testing.ScreenFixtures.SCREEN_ID;
import static com.zetcom.mpria.bff.testing.ScreenFixtures.TENANT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The configuration adapter against a stubbed config-service.
 *
 * <p>Three things are worth a test here and are invisible from the assembler: the tenant travels in
 * a header while the token travels in {@code Authorization}, a published revision is cached rather
 * than re-fetched, and config-service being unreachable is reported as such instead of as an empty
 * screen.
 */
class ConfigServiceGatewayTest {

    private static final String BASE_URL = "http://config-service:8082";

    private static final String REVISION_JSON = """
            { "revisionId": "rev-1", "publishedAt": "2026-08-01T10:15:30Z", "status": "PUBLISHED" }
            """;

    private static final String SCHEMA_JSON = """
            { "recordType": "object", "schemaVersion": 1, "titleFieldId": "title",
              "fields": [
                { "id": "inventoryNumber", "type": "STRING", "label": "config.field.inventoryNumber",
                  "required": true, "maxLength": 50, "allowedValues": [] },
                { "id": "condition", "type": "ENUM", "label": "config.field.condition",
                  "required": false, "maxLength": null, "allowedValues": ["GOOD", "FAIR", "POOR"] }
              ] }
            """;

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final RestClient.Builder builder = RestClient.builder();
    private final ConfigurationRevision pinned = new ConfigurationRevision(REVISION_ID);

    private MockRestServiceServer server;
    private ConfigServiceGateway gateway;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new ConfigServiceGateway(builder, new ConfigServiceProperties(BASE_URL));
        CallerAuthentication.install();
    }

    @AfterEach
    void tearDown() {
        CallerAuthentication.clear();
    }

    @Test
    void resolves_the_currently_published_revision() {
        server.expect(requestTo(BASE_URL + "/api/v1/revisions/current"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(REVISION_JSON, MediaType.APPLICATION_JSON));

        assertThat(gateway.currentRevision()).isEqualTo(new ConfigurationRevision("rev-1"));
        server.verify();
    }

    @Test
    void sends_the_tenant_in_a_header_and_the_callers_token_in_authorization() {
        server.expect(requestTo(BASE_URL + "/api/v1/record-types/object/schema"))
                .andExpect(header("X-Tenant-Id", TENANT_ID.toString()))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + CallerAuthentication.TOKEN))
                .andRespond(withSuccess(SCHEMA_JSON, MediaType.APPLICATION_JSON));

        gateway.schema(pinned, "object");
        server.verify();
    }

    @Test
    void maps_the_schema_payload_onto_the_domain_preserving_the_declared_option_order() {
        server.expect(requestTo(BASE_URL + "/api/v1/record-types/object/schema"))
                .andRespond(withSuccess(SCHEMA_JSON, MediaType.APPLICATION_JSON));

        RecordSchema schema = gateway.schema(pinned, "object");

        assertThat(schema.recordType()).isEqualTo("object");
        assertThat(schema.titleFieldId()).isEqualTo("title");
        assertThat(schema.field("inventoryNumber")).get()
                .satisfies(field -> assertThat(field.required()).isTrue());
        assertThat(schema.field("condition")).get()
                .satisfies(field -> assertThat(field.allowedValues())
                        .containsExactly("GOOD", "FAIR", "POOR"));
    }

    @Test
    void deserialises_the_real_pm_contract_layout_rather_than_a_parallel_shape() {
        server.expect(requestTo(BASE_URL + "/api/v1/screens/" + SCREEN_ID))
                .andRespond(withSuccess(screenPayload(), MediaType.APPLICATION_JSON));

        ScreenDefinition screen = gateway.screen(pinned, SCREEN_ID);

        assertThat(screen.recordType()).isEqualTo("object");
        assertThat(screen.layout()).isEqualTo(ScreenFixtures.layout());
    }

    @Test
    void caches_by_revision_because_a_published_revision_is_immutable() {
        server.expect(ExpectedCount.once(), requestTo(BASE_URL + "/api/v1/screens/" + SCREEN_ID))
                .andRespond(withSuccess(screenPayload(), MediaType.APPLICATION_JSON));

        assertThat(gateway.screen(pinned, SCREEN_ID)).isEqualTo(gateway.screen(pinned, SCREEN_ID));
        server.verify();
    }

    @Test
    void re_fetches_once_a_new_revision_is_published() {
        server.expect(ExpectedCount.twice(), requestTo(BASE_URL + "/api/v1/screens/" + SCREEN_ID))
                .andRespond(withSuccess(screenPayload(), MediaType.APPLICATION_JSON));

        gateway.screen(pinned, SCREEN_ID);
        gateway.screen(new ConfigurationRevision("rev-2"), SCREEN_ID);

        server.verify();
    }

    @Test
    void an_undefined_screen_is_absent_rather_than_an_empty_layout() {
        server.expect(requestTo(BASE_URL + "/api/v1/screens/nowhere"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatExceptionOfType(ScreenNotFoundException.class)
                .isThrownBy(() -> gateway.screen(pinned, "nowhere"))
                .withMessageContaining("nowhere");
    }

    @Test
    void a_token_without_a_tenant_claim_cannot_be_used_to_read_configuration() {
        CallerAuthentication.installWithoutTenant();

        assertThatExceptionOfType(IncompleteCallerTokenException.class)
                .isThrownBy(() -> gateway.schema(pinned, "object"))
                .withMessageContaining("tenant_id");
    }

    @Test
    void config_service_being_down_is_reported_as_such_and_not_as_a_broken_screen() {
        server.expect(requestTo(BASE_URL + "/api/v1/revisions/current"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatExceptionOfType(UpstreamUnavailableException.class)
                .isThrownBy(gateway::currentRevision)
                .withMessageContaining("config-service");
    }

    private String screenPayload() {
        return mapper.writeValueAsString(Map.of(
                "screenId", SCREEN_ID,
                "recordType", "object",
                "revisionId", REVISION_ID,
                "layout", ScreenFixtures.layout()));
    }
}
