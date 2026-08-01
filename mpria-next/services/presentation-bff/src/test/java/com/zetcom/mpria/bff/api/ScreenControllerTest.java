package com.zetcom.mpria.bff.api;

import java.util.List;
import java.util.Map;

import com.zetcom.mpria.bff.application.PresentationModelAssembler;
import com.zetcom.mpria.bff.application.RecordSubmission;
import com.zetcom.mpria.bff.domain.IllegalLifecycleTransitionException;
import com.zetcom.mpria.bff.domain.LayoutSchemaMismatchException;
import com.zetcom.mpria.bff.domain.Lifecycle;
import com.zetcom.mpria.bff.domain.RecordNotFoundException;
import com.zetcom.mpria.bff.infrastructure.security.SecurityConfig;
import com.zetcom.mpria.bff.testing.ScreenFixtures;
import com.zetcom.mpria.pm.ActionModel;
import com.zetcom.mpria.pm.FieldState;
import com.zetcom.mpria.pm.PresentationModel;
import com.zetcom.mpria.pm.ScreenMeta;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static com.zetcom.mpria.bff.testing.ScreenFixtures.ETAG;
import static com.zetcom.mpria.bff.testing.ScreenFixtures.RECORD_ID;
import static com.zetcom.mpria.bff.testing.ScreenFixtures.SCREEN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * The five endpoints, plus the filter chain that guards them.
 *
 * <p>The assembler is mocked here on purpose: what is under test is routing, the mapping of a path
 * variable and an {@code If-Match} header onto a use-case call, and the status a failure produces.
 * Assembly itself is tested against fakes, without a Spring context.
 *
 * <p>The write tests assert the exact {@code RecordSubmission} the use case receives, which is what
 * pins the wire name of the values map. The client sends {@code values}; a DTO that read anything else
 * would deserialise an empty payload and still return 200.
 *
 * <p>{@code JwtDecoder} is mocked because the resource-server filter chain would otherwise try to
 * fetch the realm's JWKS from the configured issuer while the test starts.
 */
@WebMvcTest(controllers = ScreenController.class)
@Import(SecurityConfig.class)
class ScreenControllerTest {

    private static final String BASE = "/api/v1/screens/" + SCREEN_ID;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private PresentationModelAssembler assembler;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void an_unauthenticated_request_is_rejected_before_it_reaches_a_controller() {
        assertThat(mvc.get().uri(BASE + "/" + RECORD_ID))
                .hasStatus(HttpStatus.UNAUTHORIZED);

        then(assembler).shouldHaveNoInteractions();
    }

    @Test
    void serves_a_populated_form() {
        given(assembler.existingRecord(SCREEN_ID, RECORD_ID)).willReturn(model(ETAG));

        assertThat(mvc.get().uri(BASE + "/" + RECORD_ID).with(jwt()))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.meta.etag").isEqualTo(ETAG);
    }

    @Test
    void serves_a_blank_form_for_the_requested_record_type() {
        given(assembler.blankForm(SCREEN_ID, "object")).willReturn(model(null));

        assertThat(mvc.get().uri(BASE + "/new").param("recordType", "object").with(jwt()))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.meta.screenId").isEqualTo(SCREEN_ID);
    }

    @Test
    void relays_the_if_match_header_as_the_writes_precondition() {
        given(assembler.submit(eq(SCREEN_ID), eq(RECORD_ID), any())).willReturn(model(ETAG));

        assertThat(mvc.put().uri(BASE + "/" + RECORD_ID)
                .header(HttpHeaders.IF_MATCH, ETAG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"INV-0001\",\"values\":{\"title\":\"Study\"}}")
                .with(jwt()))
                .hasStatus(HttpStatus.OK);

        then(assembler).should().submit(SCREEN_ID, RECORD_ID,
                new RecordSubmission("INV-0001", Map.of("title", "Study"), ETAG));
    }

    @Test
    void a_create_needs_no_record_type_because_the_screen_already_names_one() {
        given(assembler.create(eq(SCREEN_ID), any())).willReturn(model(null));

        assertThat(mvc.post().uri(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"INV-0002\",\"values\":{}}")
                .with(jwt()))
                .hasStatus(HttpStatus.OK);

        then(assembler).should().create(SCREEN_ID, new RecordSubmission("INV-0002", Map.of(), null));
    }

    @Test
    void moves_a_record_through_its_lifecycle_at_the_path_the_model_advertises() {
        given(assembler.transition(SCREEN_ID, RECORD_ID, Lifecycle.PUBLISHED)).willReturn(model(ETAG));

        assertThat(mvc.post().uri(BASE + "/" + RECORD_ID + "/lifecycle/PUBLISHED").with(jwt()))
                .hasStatus(HttpStatus.OK);

        then(assembler).should().transition(SCREEN_ID, RECORD_ID, Lifecycle.PUBLISHED);
    }

    @Test
    void a_lifecycle_state_that_does_not_exist_is_refused_without_reaching_the_use_case() {
        assertThat(mvc.post().uri(BASE + "/" + RECORD_ID + "/lifecycle/SHREDDED").with(jwt()))
                .hasStatus(HttpStatus.BAD_REQUEST);

        then(assembler).shouldHaveNoInteractions();
    }

    @Test
    void an_illegal_transition_is_a_409_problem_document() {
        willThrow(new IllegalLifecycleTransitionException(RECORD_ID, Lifecycle.PUBLISHED))
                .given(assembler).transition(SCREEN_ID, RECORD_ID, Lifecycle.PUBLISHED);

        assertThat(mvc.post().uri(BASE + "/" + RECORD_ID + "/lifecycle/PUBLISHED").with(jwt()))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.type")
                .isEqualTo("https://docs.mpria.zetcom.com/problems/illegal-transition");
    }

    @Test
    void a_missing_record_is_a_404_problem_document_rather_than_a_stack_trace() {
        willThrow(new RecordNotFoundException(RECORD_ID))
                .given(assembler).existingRecord(SCREEN_ID, RECORD_ID);

        assertThat(mvc.get().uri(BASE + "/" + RECORD_ID).with(jwt()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.type")
                .isEqualTo("https://docs.mpria.zetcom.com/problems/record-not-found");
    }

    @Test
    void a_layout_the_schema_cannot_satisfy_is_reported_as_a_server_side_configuration_fault() {
        willThrow(new LayoutSchemaMismatchException(SCREEN_ID, "object", "dominantColour"))
                .given(assembler).blankForm(SCREEN_ID, "object");

        assertThat(mvc.get().uri(BASE + "/new").param("recordType", "object").with(jwt()))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyJson().extractingPath("$.type")
                .isEqualTo("https://docs.mpria.zetcom.com/problems/layout-schema-mismatch");
    }

    /**
     * The actuator endpoint itself is not mapped in a slice test, so what this asserts is the part
     * that belongs to the filter chain: the request reaches the dispatcher instead of being turned
     * away at the gate. An orchestrator holds no token and still has to be able to probe.
     */
    @Test
    void the_health_probe_stays_open_so_the_orchestrator_can_reach_it_without_a_token() {
        assertThat(mvc.get().uri("/actuator/health").exchange().getResponse().getStatus())
                .isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private static PresentationModel model(String etag) {
        return new PresentationModel(
                new ScreenMeta(SCREEN_ID, ScreenFixtures.REVISION_ID,
                        etag == null ? null : RECORD_ID.toString(), etag, List.of("EDIT")),
                ScreenFixtures.layout(),
                Map.of("inventoryNumber", FieldState.editable("STRING", "config.field.inventoryNumber", null),
                        "title", FieldState.editable("STRING", "config.field.title", null),
                        "condition", FieldState.editable("ENUM", "config.field.condition", null)),
                List.of(ActionModel.enabled("save", "action.save", "PUT", BASE + "/" + RECORD_ID)));
    }
}
