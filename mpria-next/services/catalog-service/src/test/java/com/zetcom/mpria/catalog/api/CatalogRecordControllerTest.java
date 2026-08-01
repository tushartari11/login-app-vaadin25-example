package com.zetcom.mpria.catalog.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.zetcom.mpria.catalog.application.CatalogRecordService;
import com.zetcom.mpria.catalog.application.StaleRecordException;
import com.zetcom.mpria.catalog.application.UnknownRecordTypeException;
import com.zetcom.mpria.catalog.domain.CatalogFixtures;
import com.zetcom.mpria.catalog.domain.CatalogRecord;
import com.zetcom.mpria.catalog.domain.RecordNotFoundException;
import com.zetcom.mpria.catalog.domain.RecordValidationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * The HTTP contract of the record API.
 *
 * <p>The application layer is mocked on purpose: what is under test here is translation — status
 * codes, ETags, preconditions and problem documents — not business rules, which have their own
 * fast tests in {@code domain} and {@code application}.
 */
@WebMvcTest(CatalogRecordController.class)
class CatalogRecordControllerTest {

    private static final UUID RECORD_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    CatalogRecordService catalog;

    /** The resource-server filter chain needs a decoder even though no real token is parsed. */
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void rejects_an_unauthenticated_request_before_it_reaches_the_use_case() {
        assertThat(mvc.get().uri("/api/v1/records/{id}", RECORD_ID))
                .hasStatus(401);
    }

    @Test
    void returns_a_record_with_its_etag_so_the_client_can_write_safely() {
        given(catalog.get(any(), eq(RECORD_ID))).willReturn(record());

        assertThat(mvc.get().uri("/api/v1/records/{id}", RECORD_ID).with(curator()))
                .hasStatusOk()
                .hasHeader(HttpHeaders.ETAG, "\"0\"")
                .bodyJson().extractingPath("$.title").isEqualTo("Study of a hand");
    }

    @Test
    void creating_returns_201_with_a_location_and_an_etag() {
        given(catalog.create(any(), eq("object"), eq("INV-0001"), any())).willReturn(record());

        assertThat(mvc.post().uri("/api/v1/records")
                .with(curator())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"recordType":"object","identifier":"INV-0001",
                         "data":{"inventoryNumber":"INV-0001","title":"Study of a hand"}}"""))
                .hasStatus(201)
                .hasHeader(HttpHeaders.ETAG, "\"0\"")
                .hasHeader(HttpHeaders.LOCATION, "/api/v1/records/" + RECORD_ID);
    }

    /**
     * The single most important response in the API: a payload that fails its configured schema
     * comes back as structured, per-field data. The presentation model feeds this straight into
     * {@code FieldState.errors}, so prose would be useless to it.
     */
    @Test
    void a_schema_violation_is_422_with_errors_keyed_by_field() {
        willThrow(new RecordValidationException("object",
                Map.of("inventoryNumber", List.of("is required"))))
                .given(catalog).create(any(), any(), any(), any());

        assertThat(mvc.post().uri("/api/v1/records")
                .with(curator())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"recordType":"object","identifier":null,"data":{}}"""))
                .hasStatus(422)
                .bodyJson()
                .extractingPath("$.errors.inventoryNumber[0]").isEqualTo("is required");
    }

    @Test
    void the_if_match_header_reaches_the_use_case_as_the_expected_etag() {
        given(catalog.update(any(), eq(RECORD_ID), any(), any(), eq("\"3\""))).willReturn(record());

        assertThat(mvc.put().uri("/api/v1/records/{id}", RECORD_ID)
                .with(curator())
                .header(HttpHeaders.IF_MATCH, "\"3\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"identifier":"INV-0001","data":{"inventoryNumber":"INV-0001","title":"t"}}"""))
                .hasStatusOk();
    }

    @Test
    void a_write_from_a_stale_copy_is_412_not_409() {
        willThrow(new StaleRecordException(RECORD_ID, "\"1\"", "\"4\""))
                .given(catalog).update(any(), any(), any(), any(), any());

        assertThat(mvc.put().uri("/api/v1/records/{id}", RECORD_ID)
                .with(curator())
                .header(HttpHeaders.IF_MATCH, "\"1\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"identifier":"INV-0001","data":{}}"""))
                .hasStatus(412)
                .bodyJson().extractingPath("$.type").asString().endsWith("/stale-record");
    }

    @Test
    void a_missing_record_is_404_as_a_problem_document() {
        willThrow(new RecordNotFoundException(RECORD_ID)).given(catalog).get(any(), any());

        assertThat(mvc.get().uri("/api/v1/records/{id}", RECORD_ID).with(curator()))
                .hasStatus(404)
                .bodyJson().extractingPath("$.type").asString().endsWith("/record-not-found");
    }

    @Test
    void an_unconfigured_record_type_is_a_client_error_not_a_server_error() {
        willThrow(new UnknownRecordTypeException("spacecraft"))
                .given(catalog).create(any(), any(), any(), any());

        assertThat(mvc.post().uri("/api/v1/records")
                .with(curator())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"recordType":"spacecraft","identifier":"S-1","data":{}}"""))
                .hasStatus(400)
                .bodyJson().extractingPath("$.type").asString().endsWith("/unknown-record-type");
    }

    /**
     * A token can be perfectly valid and still be unusable: without the tenant claim the service
     * cannot know who it is acting for, and guessing would be a cross-tenant write.
     */
    @Test
    void a_valid_token_missing_the_tenant_claim_is_403_rather_than_a_500() {
        assertThat(mvc.get().uri("/api/v1/records/{id}", RECORD_ID)
                .with(jwt().jwt(token -> token.subject("curator@example.org"))))
                .hasStatus(403)
                .bodyJson().extractingPath("$.type").asString().endsWith("/incomplete-token");
    }

    @Test
    void bean_validation_rejects_a_blank_record_type_before_the_use_case_runs() {
        assertThat(mvc.post().uri("/api/v1/records")
                .with(curator())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"recordType":"","identifier":"X","data":{}}"""))
                .hasStatus(400);
    }

    /**
     * Builds a token carrying the claims the service needs.
     *
     * <p>Note that {@code jwt()} does not run the application's {@code JwtAuthenticationConverter},
     * so any authority-based gating must be asserted by passing authorities explicitly rather than
     * assumed from the claims here.
     */
    private static org.springframework.test.web.servlet.request.RequestPostProcessor curator() {
        return jwt().jwt(token -> token
                .subject("curator@example.org")
                .claim(JwtPrincipals.TENANT_CLAIM, CatalogFixtures.TENANT.toString())
                .claim(JwtPrincipals.ORG_UNIT_CLAIM, CatalogFixtures.ORG_UNIT.toString()));
    }

    private static CatalogRecord record() {
        return CatalogRecord.rehydrate(RECORD_ID, CatalogFixtures.TENANT, "object",
                CatalogFixtures.ORG_UNIT, 1, "INV-0001", "Study of a hand",
                com.zetcom.mpria.catalog.domain.Lifecycle.DRAFT,
                Map.of("inventoryNumber", "INV-0001", "title", "Study of a hand"),
                CatalogFixtures.NOW, "curator@example.org",
                CatalogFixtures.NOW, "curator@example.org", 0L);
    }
}
