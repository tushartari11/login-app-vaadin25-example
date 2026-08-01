package com.zetcom.mpria.bff.infrastructure.catalogclient;

import java.util.Map;
import java.util.UUID;

import com.zetcom.mpria.bff.application.RecordSubmission;
import com.zetcom.mpria.bff.application.UpstreamUnavailableException;
import com.zetcom.mpria.bff.application.port.CatalogGateway.WriteResult;
import com.zetcom.mpria.bff.domain.IllegalLifecycleTransitionException;
import com.zetcom.mpria.bff.domain.Lifecycle;
import com.zetcom.mpria.bff.domain.RecordNotFoundException;
import com.zetcom.mpria.bff.domain.RecordSnapshot;
import com.zetcom.mpria.bff.domain.StaleRecordException;
import com.zetcom.mpria.bff.testing.CallerAuthentication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static com.zetcom.mpria.bff.testing.ScreenFixtures.ETAG;
import static com.zetcom.mpria.bff.testing.ScreenFixtures.RECORD_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The catalogue adapter against a stubbed catalog-service.
 *
 * <p>The behaviour that matters is the translation of a 422 problem document into a rejection that
 * carries field errors. Everything upstream of it — the assembler turning that rejection into a
 * re-rendered form — depends on this method not throwing.
 */
class CatalogServiceGatewayTest {

    private static final String BASE_URL = "http://catalog-service:8081";
    private static final String RECORDS = BASE_URL + "/api/v1/records";

    private static final String RECORD_JSON = """
            { "id": "11111111-2222-3333-4444-555555555555", "recordType": "object",
              "schemaVersion": 1, "identifier": "INV-0001", "title": "Study of a hand",
              "lifecycle": "DRAFT",
              "data": { "inventoryNumber": "INV-0001", "title": "Study of a hand" },
              "etag": "\\"7\\"",
              "createdAt": "2026-08-01T10:00:00Z", "createdBy": "curator@example.org",
              "updatedAt": "2026-08-01T10:05:00Z", "updatedBy": "curator@example.org" }
            """;

    private static final String PUBLISHED_RECORD_JSON = RECORD_JSON
            .replace("\"lifecycle\": \"DRAFT\"", "\"lifecycle\": \"PUBLISHED\"");

    private static final String INVALID_RECORD_JSON = """
            { "type": "https://docs.mpria.zetcom.com/problems/invalid-record",
              "status": 422,
              "errors": { "inventoryNumber": ["is required"],
                          "acquiredOn": ["must be an ISO-8601 date (yyyy-MM-dd)"] } }
            """;

    private final RestClient.Builder builder = RestClient.builder();

    private MockRestServiceServer server;
    private CatalogServiceGateway gateway;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new CatalogServiceGateway(builder, new CatalogServiceProperties(BASE_URL));
        CallerAuthentication.install();
    }

    @AfterEach
    void tearDown() {
        CallerAuthentication.clear();
    }

    @Test
    void reads_a_record_and_keeps_the_etag_the_next_write_will_need() {
        server.expect(requestTo(RECORDS + "/" + RECORD_ID))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + CallerAuthentication.TOKEN))
                .andRespond(withSuccess(RECORD_JSON, MediaType.APPLICATION_JSON));

        RecordSnapshot record = gateway.get(RECORD_ID);

        assertThat(record.id()).isEqualTo(RECORD_ID);
        assertThat(record.lifecycle()).isEqualTo(Lifecycle.DRAFT);
        assertThat(record.etag()).isEqualTo(ETAG);
        assertThat(record.data()).containsEntry("title", "Study of a hand");
    }

    @Test
    void a_write_carries_the_callers_etag_as_an_if_match_precondition() {
        server.expect(requestTo(RECORDS + "/" + RECORD_ID))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.IF_MATCH, ETAG))
                .andExpect(jsonPath("$.identifier").value("INV-0001"))
                .andRespond(withSuccess(RECORD_JSON, MediaType.APPLICATION_JSON));

        gateway.update(RECORD_ID, new RecordSubmission("INV-0001", Map.of("title", "Study"), ETAG));

        server.verify();
    }

    @Test
    void a_create_names_the_record_type_because_a_new_record_has_no_identity_yet() {
        server.expect(requestTo(RECORDS))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.recordType").value("object"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body(RECORD_JSON));

        WriteResult result = gateway.create("object", new RecordSubmission("INV-0001", Map.of(), null));

        assertThat(result).asInstanceOf(type(WriteResult.Stored.class))
                .satisfies(stored -> assertThat(stored.record().identifier()).isEqualTo("INV-0001"));
    }

    @Test
    void a_validation_failure_is_a_rejection_carrying_field_errors_not_an_exception() {
        server.expect(requestTo(RECORDS + "/" + RECORD_ID))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(INVALID_RECORD_JSON));

        WriteResult result = gateway.update(RECORD_ID, new RecordSubmission("INV-0001", Map.of(), ETAG));

        assertThat(result).asInstanceOf(type(WriteResult.Rejected.class))
                .satisfies(rejected -> {
                    assertThat(rejected.errors().messagesFor("inventoryNumber"))
                            .containsExactly("is required");
                    assertThat(rejected.errors().messagesFor("acquiredOn"))
                            .containsExactly("must be an ISO-8601 date (yyyy-MM-dd)");
                });
    }

    @Test
    void a_validation_failure_with_no_errors_object_still_yields_a_rejection() {
        server.expect(requestTo(RECORDS))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":422}"));

        WriteResult result = gateway.create("object", new RecordSubmission(null, Map.of(), null));

        assertThat(result).asInstanceOf(type(WriteResult.Rejected.class))
                .satisfies(rejected -> assertThat(rejected.errors().isEmpty()).isTrue());
    }

    @Test
    void a_missing_record_is_not_found_rather_than_an_unavailable_upstream() {
        UUID unknown = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        server.expect(requestTo(RECORDS + "/" + unknown))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatExceptionOfType(RecordNotFoundException.class)
                .isThrownBy(() -> gateway.get(unknown))
                .withMessageContaining(unknown.toString());
    }

    /**
     * A stale write is not a validation failure: re-rendering the caller's values would quietly
     * discard whoever won the race. The caller has to re-read, so this one propagates.
     */
    @Test
    void a_stale_precondition_is_surfaced_rather_than_swallowed_into_a_re_rendered_form() {
        server.expect(requestTo(RECORDS + "/" + RECORD_ID))
                .andRespond(withStatus(HttpStatus.PRECONDITION_FAILED));

        assertThatExceptionOfType(StaleRecordException.class)
                .isThrownBy(() -> gateway.update(RECORD_ID,
                        new RecordSubmission("INV-0001", Map.of(), "\"1\"")))
                .withMessageContaining(RECORD_ID.toString());
    }

    @Test
    void moves_a_record_to_another_lifecycle_state() {
        server.expect(requestTo(RECORDS + "/" + RECORD_ID + "/lifecycle/PUBLISHED"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + CallerAuthentication.TOKEN))
                .andRespond(withSuccess(PUBLISHED_RECORD_JSON, MediaType.APPLICATION_JSON));

        RecordSnapshot moved = gateway.transition(RECORD_ID, Lifecycle.PUBLISHED);

        assertThat(moved.lifecycle()).isEqualTo(Lifecycle.PUBLISHED);
        server.verify();
    }

    /**
     * catalog-service answers both an illegal transition and a lost write race with a 409. They mean
     * different things to the user — "that move is not allowed" against "reload, somebody else won" —
     * so the problem document's {@code type} is what tells them apart, not the status.
     */
    @Test
    void a_refused_transition_is_reported_as_illegal_rather_than_as_a_stale_form() {
        server.expect(requestTo(RECORDS + "/" + RECORD_ID + "/lifecycle/PUBLISHED"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                { "type": "https://docs.mpria.zetcom.com/problems/illegal-transition",
                                  "status": 409, "detail": "ARCHIVED is terminal" }
                                """));

        assertThatExceptionOfType(IllegalLifecycleTransitionException.class)
                .isThrownBy(() -> gateway.transition(RECORD_ID, Lifecycle.PUBLISHED))
                .withMessageContaining("PUBLISHED");
    }

    @Test
    void a_transition_that_lost_a_write_race_is_still_reported_as_stale() {
        server.expect(requestTo(RECORDS + "/" + RECORD_ID + "/lifecycle/ARCHIVED"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                { "type": "https://docs.mpria.zetcom.com/problems/concurrent-modification",
                                  "status": 409 }
                                """));

        assertThatExceptionOfType(StaleRecordException.class)
                .isThrownBy(() -> gateway.transition(RECORD_ID, Lifecycle.ARCHIVED));
    }

    @Test
    void catalog_service_being_down_is_reported_as_such() {
        server.expect(requestTo(RECORDS + "/" + RECORD_ID))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatExceptionOfType(UpstreamUnavailableException.class)
                .isThrownBy(() -> gateway.get(RECORD_ID))
                .withMessageContaining("catalog-service");
    }
}
