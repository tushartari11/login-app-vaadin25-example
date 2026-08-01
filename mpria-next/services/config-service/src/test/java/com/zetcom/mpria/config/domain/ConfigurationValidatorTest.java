package com.zetcom.mpria.config.domain;

import java.util.List;

import com.zetcom.mpria.config.testing.ConfigFixtures;
import com.zetcom.mpria.pm.GridSpec;
import com.zetcom.mpria.pm.Layout;
import com.zetcom.mpria.pm.LayoutNode;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * The publish gate.
 *
 * <p>This is the reason config-service exists as a service rather than a JSON file: a configuration
 * that would render a broken screen must be refused at publish time, while the SME who wrote it is
 * still looking at it. Every assertion below therefore pins the <em>message</em>, not just the
 * rejection — an error that does not name the offending field is not actionable.
 */
class ConfigurationValidatorTest {

    private final ConfigurationValidator validator = new ConfigurationValidator(ComponentRegistry.standard());

    @Test
    void accepts_a_revision_whose_every_binding_resolves() {
        assertThat(validator.validate(ConfigFixtures.draftRevision())).isEmpty();
    }

    @Nested
    class RejectsAnUnresolvableFieldBinding {

        @Test
        void naming_the_field_and_the_record_type_that_lacks_it() {
            ConfigurationRevision revision = revisionWith(
                    ConfigFixtures.screenBinding("slot-x", "TEXT_FIELD", "provenance"));

            assertThat(validator.validate(revision)).containsExactly(new ConfigurationError(
                    "object-detail", "slot-x",
                    "binds field 'provenance', which record type 'object' does not define"));
        }

        @Test
        void naming_every_offending_field_rather_than_only_the_first() {
            LayoutNode.Region main = new LayoutNode.Region("main", GridSpec.singleColumn(), List.of(
                    ConfigFixtures.slot("slot-a", "TEXT_FIELD", "provenance", null),
                    ConfigFixtures.slot("slot-b", "TEXT_FIELD", "creditLine", null)));
            ConfigurationRevision revision = revisionWith(new ScreenLayout("object-detail", "object",
                    new Layout("object-detail", "object", List.of(main))));

            assertThat(validator.validate(revision))
                    .extracting(ConfigurationError::message)
                    .containsExactly(
                            "binds field 'provenance', which record type 'object' does not define",
                            "binds field 'creditLine', which record type 'object' does not define");
        }
    }

    @Test
    void rejects_a_component_type_that_is_not_in_the_registry() {
        ConfigurationRevision revision = revisionWith(
                ConfigFixtures.screenBinding("slot-x", "CAROUSEL", "title"));

        assertThat(validator.validate(revision)).containsExactly(new ConfigurationError(
                "object-detail", "slot-x",
                "uses component type 'CAROUSEL', which is not in the component registry"));
    }

    @Nested
    class RejectsAComponentThatCannotDrawItsField {

        @Test
        void when_the_field_type_is_outside_the_components_accepted_types() {
            ConfigurationRevision revision = revisionWith(
                    ConfigFixtures.screenBinding("slot-x", "TEXT_FIELD", "acquiredOn"));

            assertThat(validator.validate(revision)).containsExactly(new ConfigurationError(
                    "object-detail", "slot-x",
                    "binds field 'acquiredOn' of type DATE to component 'TEXT_FIELD', "
                            + "which accepts [STRING]"));
        }

        @Test
        void when_the_component_takes_no_field_binding_at_all() {
            ConfigurationRevision revision = revisionWith(
                    ConfigFixtures.screenBinding("slot-x", "MEDIA_GALLERY", "title"));

            assertThat(validator.validate(revision)).containsExactly(new ConfigurationError(
                    "object-detail", "slot-x",
                    "binds field 'title' to component 'MEDIA_GALLERY', which accepts no field"));
        }

        @Test
        void when_a_component_that_needs_a_field_was_left_unbound() {
            ConfigurationRevision revision = revisionWith(
                    ConfigFixtures.screenBinding("slot-x", "TEXT_FIELD", null));

            assertThat(validator.validate(revision)).containsExactly(new ConfigurationError(
                    "object-detail", "slot-x",
                    "uses component 'TEXT_FIELD', which requires a field binding"));
        }
    }

    @Test
    void rejects_a_screen_whose_record_type_the_revision_does_not_define() {
        Layout layout = new Layout("acquisition-detail", "acquisition", List.of(
                new LayoutNode.Region("main", GridSpec.singleColumn(), List.of())));
        ConfigurationRevision revision = revisionWith(
                new ScreenLayout("acquisition-detail", "acquisition", layout));

        assertThat(validator.validate(revision)).containsExactly(new ConfigurationError(
                "acquisition-detail", null,
                "references record type 'acquisition', which this revision does not define"));
    }

    @Test
    void reports_nothing_about_the_bindings_of_a_screen_whose_record_type_is_unknown() {
        Layout layout = new Layout("acquisition-detail", "acquisition", List.of(
                new LayoutNode.Region("main", GridSpec.singleColumn(),
                        List.of(ConfigFixtures.slot("slot-x", "TEXT_FIELD", "anything", null)))));
        ConfigurationRevision revision = revisionWith(
                new ScreenLayout("acquisition-detail", "acquisition", layout));

        // One cause, one error. Cascading a field report off a missing record type buries the fix.
        assertThat(validator.validate(revision)).hasSize(1);
    }

    @Nested
    class RequireValid {

        @Test
        void throws_carrying_every_error_so_the_console_can_list_them() {
            ConfigurationRevision revision = revisionWith(
                    ConfigFixtures.screenBinding("slot-x", "CAROUSEL", "provenance"));

            assertThatExceptionOfType(ConfigurationValidationException.class)
                    .isThrownBy(() -> validator.requireValid(revision))
                    .satisfies(e -> assertThat(e.errors()).hasSize(1))
                    .withMessageContaining("1 problem");
        }

        @Test
        void passes_a_revision_that_validates() {
            validator.requireValid(ConfigFixtures.draftRevision());
        }
    }

    private static ConfigurationRevision revisionWith(ScreenLayout screen) {
        return ConfigurationRevision.draft(
                "draft-1", List.of(ConfigFixtures.objectRecordType()), List.of(screen));
    }
}
