package com.zetcom.mpria.bff.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.zetcom.mpria.bff.domain.LayoutSchemaMismatchException;
import com.zetcom.mpria.bff.domain.Lifecycle;
import com.zetcom.mpria.bff.domain.RecordNotFoundException;
import com.zetcom.mpria.bff.domain.RecordTypeMismatchException;
import com.zetcom.mpria.bff.testing.FakeCatalogGateway;
import com.zetcom.mpria.bff.testing.FakeConfigurationGateway;
import com.zetcom.mpria.bff.testing.ScreenFixtures;
import com.zetcom.mpria.pm.ActionModel;
import com.zetcom.mpria.pm.FieldOption;
import com.zetcom.mpria.pm.FieldState;
import com.zetcom.mpria.pm.PresentationModel;

import org.junit.jupiter.api.Test;

import static com.zetcom.mpria.bff.testing.ScreenFixtures.ETAG;
import static com.zetcom.mpria.bff.testing.ScreenFixtures.RECORD_ID;
import static com.zetcom.mpria.bff.testing.ScreenFixtures.RECORD_TYPE;
import static com.zetcom.mpria.bff.testing.ScreenFixtures.REVISION_ID;
import static com.zetcom.mpria.bff.testing.ScreenFixtures.SCREEN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Assembly tests against fakes: no Spring context, no HTTP, milliseconds to run.
 *
 * <p>What is asserted here is the six assembly rules of {@code docs/CONTRACTS.md} — which fields
 * appear, which actions are offered, and what happens when the write is refused. Those are the
 * whole behaviour of this service; everything else in it is transport.
 */
class PresentationModelAssemblerTest {

    private final FakeConfigurationGateway configuration = new FakeConfigurationGateway()
            .with(ScreenFixtures.screen())
            .with(ScreenFixtures.schema());
    private final FakeCatalogGateway catalog = new FakeCatalogGateway();

    private final PresentationModelAssembler assembler =
            new PresentationModelAssembler(configuration, catalog);

    // ---------------------------------------------------------------- populated form

    @Test
    void renders_a_populated_form_from_configuration_and_data() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT));

        PresentationModel model = assembler.existingRecord(SCREEN_ID, RECORD_ID);

        assertThat(model.meta().screenId()).isEqualTo(SCREEN_ID);
        assertThat(model.meta().revisionId()).isEqualTo(REVISION_ID);
        assertThat(model.meta().recordId()).isEqualTo(RECORD_ID.toString());
        assertThat(model.layout()).isEqualTo(ScreenFixtures.layout());
        assertThat(model.fields()).extractingByKey("title")
                .extracting(FieldState::value).isEqualTo("Study of a hand");
    }

    @Test
    void contains_exactly_the_fields_the_layout_binds_and_not_the_whole_schema() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT));

        PresentationModel model = assembler.existingRecord(SCREEN_ID, RECORD_ID);

        assertThat(model.fields()).containsOnlyKeys(ScreenFixtures.layout().boundFieldIds().toArray(String[]::new));
    }

    @Test
    void merges_schema_metadata_onto_each_bound_field() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT));

        Map<String, FieldState> fields = assembler.existingRecord(SCREEN_ID, RECORD_ID).fields();

        assertThat(fields.get("inventoryNumber").type()).isEqualTo("STRING");
        assertThat(fields.get("inventoryNumber").label()).isEqualTo("config.field.inventoryNumber");
        assertThat(fields.get("inventoryNumber").required()).isTrue();
        assertThat(fields.get("title").required()).isFalse();
        assertThat(fields.get("condition").options())
                .containsExactly(new FieldOption("GOOD", "GOOD"),
                        new FieldOption("FAIR", "FAIR"),
                        new FieldOption("POOR", "POOR"));
    }

    @Test
    void the_returned_etag_is_the_records_etag_so_the_next_write_can_use_it_as_if_match() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT));

        assertThat(assembler.existingRecord(SCREEN_ID, RECORD_ID).meta().etag()).isEqualTo(ETAG);
    }

    @Test
    void a_missing_record_reports_not_found_rather_than_an_empty_form() {
        assertThatExceptionOfType(RecordNotFoundException.class)
                .isThrownBy(() -> assembler.existingRecord(SCREEN_ID, UUID.randomUUID()));
    }

    // ---------------------------------------------------------------- blank form

    @Test
    void renders_a_blank_form_with_a_state_for_every_bound_field_and_no_values() {
        PresentationModel model = assembler.blankForm(SCREEN_ID, RECORD_TYPE);

        assertThat(model.fields()).hasSize(3);
        assertThat(model.fields().values()).allSatisfy(field -> assertThat(field.value()).isNull());
        assertThat(model.meta().recordId()).isNull();
        assertThat(model.meta().etag()).isNull();
    }

    @Test
    void a_blank_form_offers_only_save_because_there_is_no_record_to_move_through_a_lifecycle() {
        PresentationModel model = assembler.blankForm(SCREEN_ID, RECORD_TYPE);

        assertThat(model.actions()).singleElement().satisfies(action -> {
            assertThat(action.id()).isEqualTo("save");
            assertThat(action.method()).isEqualTo("POST");
            assertThat(action.href()).isEqualTo("/api/v1/screens/object-detail");
        });
    }

    @Test
    void a_blank_form_for_the_wrong_record_type_is_refused_rather_than_silently_reinterpreted() {
        assertThatExceptionOfType(RecordTypeMismatchException.class)
                .isThrownBy(() -> assembler.blankForm(SCREEN_ID, "exhibition"))
                .withMessageContaining("exhibition")
                .withMessageContaining(RECORD_TYPE);
    }

    // ---------------------------------------------------------------- actions per lifecycle

    @Test
    void save_is_always_offered_and_targets_this_screens_record() {
        catalog.with(ScreenFixtures.record(Lifecycle.ARCHIVED));

        assertThat(actionsOf(Lifecycle.ARCHIVED)).anySatisfy(action -> {
            assertThat(action.id()).isEqualTo("save");
            assertThat(action.method()).isEqualTo("PUT");
            assertThat(action.href()).isEqualTo("/api/v1/screens/object-detail/" + RECORD_ID);
        });
    }

    @Test
    void publish_is_enabled_only_while_the_record_is_a_draft() {
        assertThat(action(Lifecycle.DRAFT, "publish").enabled()).isTrue();
        assertThat(action(Lifecycle.PUBLISHED, "publish").enabled()).isFalse();
        assertThat(action(Lifecycle.ARCHIVED, "publish").enabled()).isFalse();
    }

    @Test
    void a_disabled_publish_explains_itself_so_the_ui_never_shows_an_inert_button() {
        assertThat(action(Lifecycle.PUBLISHED, "publish").disabledReason())
                .isEqualTo("record.alreadyPublished");
        assertThat(action(Lifecycle.ARCHIVED, "publish").disabledReason())
                .isEqualTo("record.alreadyArchived");
    }

    @Test
    void archive_is_disabled_with_a_reason_once_the_record_is_already_archived() {
        ActionModel archive = action(Lifecycle.ARCHIVED, "archive");

        assertThat(archive.enabled()).isFalse();
        assertThat(archive.disabledReason()).isEqualTo("record.alreadyArchived");
    }

    @Test
    void archive_always_asks_for_confirmation_because_it_takes_a_record_out_of_circulation() {
        assertThat(action(Lifecycle.DRAFT, "archive").confirm()).isTrue();
        assertThat(action(Lifecycle.ARCHIVED, "archive").confirm()).isTrue();
        assertThat(action(Lifecycle.DRAFT, "save").confirm()).isFalse();
    }

    @Test
    void an_archived_records_fields_come_back_read_only_and_without_the_edit_capability() {
        catalog.with(ScreenFixtures.record(Lifecycle.ARCHIVED));

        PresentationModel model = assembler.existingRecord(SCREEN_ID, RECORD_ID);

        assertThat(model.fields().values()).allSatisfy(field -> assertThat(field.readOnly()).isTrue());
        assertThat(model.meta().capabilities()).isEmpty();
    }

    @Test
    void an_editable_record_advertises_the_edit_capability() {
        catalog.with(ScreenFixtures.record(Lifecycle.PUBLISHED));

        assertThat(assembler.existingRecord(SCREEN_ID, RECORD_ID).meta().capabilities())
                .containsExactly("EDIT");
    }

    // ---------------------------------------------------------------- 422 is a form, not an error

    @Test
    void a_rejected_update_comes_back_as_a_form_with_the_message_on_the_offending_field() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT))
                .rejecting(Map.of("inventoryNumber", List.of("is required")));

        PresentationModel model = assembler.submit(SCREEN_ID, RECORD_ID,
                new RecordSubmission("INV-0001", Map.of("title", "Untitled"), ETAG));

        assertThat(model.fields().get("inventoryNumber").errors()).containsExactly("is required");
        assertThat(model.fields().get("title").errors()).isEmpty();
    }

    @Test
    void a_rejected_update_keeps_what_the_user_typed_so_the_form_is_not_silently_emptied() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT))
                .rejecting(Map.of("inventoryNumber", List.of("is required")));

        PresentationModel model = assembler.submit(SCREEN_ID, RECORD_ID,
                new RecordSubmission("INV-0001", Map.of("title", "Half-finished edit"), ETAG));

        assertThat(model.fields().get("title").value()).isEqualTo("Half-finished edit");
        assertThat(model.fields().get("inventoryNumber").value()).isNull();
    }

    @Test
    void a_rejected_update_still_carries_the_servers_etag_so_the_retry_can_succeed() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT))
                .rejecting(Map.of("inventoryNumber", List.of("is required")));

        PresentationModel model = assembler.submit(SCREEN_ID, RECORD_ID,
                new RecordSubmission("INV-0001", Map.of(), ETAG));

        assertThat(model.meta().etag()).isEqualTo(ETAG);
        assertThat(model.meta().recordId()).isEqualTo(RECORD_ID.toString());
    }

    @Test
    void a_rejected_create_comes_back_as_a_blank_screens_form_with_errors_attached() {
        catalog.rejecting(Map.of("inventoryNumber", List.of("is required")));

        PresentationModel model = assembler.create(SCREEN_ID,
                new RecordSubmission(null, Map.of("title", "Untitled"), null));

        assertThat(model.fields().get("inventoryNumber").errors()).containsExactly("is required");
        assertThat(model.meta().recordId()).isNull();
        assertThat(model.actions()).singleElement()
                .satisfies(action -> assertThat(action.method()).isEqualTo("POST"));
    }

    @Test
    void an_error_on_a_field_the_layout_does_not_bind_is_dropped_rather_than_breaking_the_form() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT))
                .rejecting(Map.of("inventoryNumber", List.of("is required"),
                        "acquiredOn", List.of("must be an ISO-8601 date (yyyy-MM-dd)")));

        PresentationModel model = assembler.submit(SCREEN_ID, RECORD_ID,
                new RecordSubmission("INV-0001", Map.of(), ETAG));

        assertThat(model.fields()).doesNotContainKey("acquiredOn");
        assertThat(model.fields().get("inventoryNumber").errors()).containsExactly("is required");
    }

    @Test
    void an_accepted_update_relays_the_callers_etag_as_the_precondition() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT));

        assembler.submit(SCREEN_ID, RECORD_ID, new RecordSubmission("INV-0001", Map.of(), ETAG));

        assertThat(catalog.submissions()).singleElement()
                .satisfies(submission -> assertThat(submission.etag()).isEqualTo(ETAG));
    }

    @Test
    void an_accepted_update_re_renders_from_what_was_stored_not_from_what_was_sent() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT));

        PresentationModel model = assembler.submit(SCREEN_ID, RECORD_ID,
                new RecordSubmission("INV-0001", Map.of("condition", "FAIR"), ETAG));

        assertThat(model.fields().get("condition").value()).isEqualTo("FAIR");
        assertThat(model.fields().get("inventoryNumber").errors()).isEmpty();
    }

    // ---------------------------------------------------------------- lifecycle transitions

    @Test
    void a_transition_re_renders_the_screen_from_the_moved_record() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT));

        PresentationModel model = assembler.transition(SCREEN_ID, RECORD_ID, Lifecycle.PUBLISHED);

        assertThat(model.fields()).hasSize(3);
        assertThat(model.fields().get("title").value()).isEqualTo("Study of a hand");
        assertThat(model.meta().etag()).isEqualTo(FakeCatalogGateway.ETAG_AFTER_TRANSITION);
    }

    @Test
    void the_actions_offered_after_a_transition_reflect_the_state_the_record_moved_to() {
        catalog.with(ScreenFixtures.record(Lifecycle.DRAFT));

        PresentationModel model = assembler.transition(SCREEN_ID, RECORD_ID, Lifecycle.PUBLISHED);

        assertThat(actionOf(model, "publish").enabled()).isFalse();
        assertThat(actionOf(model, "publish").disabledReason()).isEqualTo("record.alreadyPublished");
        assertThat(actionOf(model, "archive").enabled()).isTrue();
    }

    @Test
    void archiving_returns_a_read_only_form_that_cannot_be_archived_again() {
        catalog.with(ScreenFixtures.record(Lifecycle.PUBLISHED));

        PresentationModel model = assembler.transition(SCREEN_ID, RECORD_ID, Lifecycle.ARCHIVED);

        assertThat(model.fields().values()).allSatisfy(field -> assertThat(field.readOnly()).isTrue());
        assertThat(model.meta().capabilities()).isEmpty();
        assertThat(actionOf(model, "archive").disabledReason()).isEqualTo("record.alreadyArchived");
    }

    // ---------------------------------------------------------------- misconfiguration

    @Test
    void a_layout_binding_a_field_the_schema_does_not_define_fails_loudly() {
        FakeConfigurationGateway broken = new FakeConfigurationGateway()
                .with(ScreenFixtures.screenBindingAnUnknownField())
                .with(ScreenFixtures.schema());
        PresentationModelAssembler brokenAssembler = new PresentationModelAssembler(broken, catalog);

        assertThatExceptionOfType(LayoutSchemaMismatchException.class)
                .isThrownBy(() -> brokenAssembler.blankForm(SCREEN_ID, RECORD_TYPE))
                .withMessageContaining("dominantColour")
                .withMessageContaining(SCREEN_ID);
    }

    // ---------------------------------------------------------------- helpers

    private List<ActionModel> actionsOf(Lifecycle lifecycle) {
        catalog.with(ScreenFixtures.record(lifecycle));
        return assembler.existingRecord(SCREEN_ID, RECORD_ID).actions();
    }

    private ActionModel action(Lifecycle lifecycle, String actionId) {
        catalog.with(ScreenFixtures.record(lifecycle));
        return actionOf(assembler.existingRecord(SCREEN_ID, RECORD_ID), actionId);
    }

    private static ActionModel actionOf(PresentationModel model, String actionId) {
        return model.actions().stream()
                .filter(action -> action.id().equals(actionId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no '" + actionId + "' action was offered"));
    }
}
