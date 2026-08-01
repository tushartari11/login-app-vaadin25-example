package com.zetcom.mpria.config.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.zetcom.mpria.config.domain.ComponentRegistry;
import com.zetcom.mpria.config.domain.ConfigurationRevision;
import com.zetcom.mpria.config.domain.ConfigurationValidationException;
import com.zetcom.mpria.config.domain.ConfigurationValidator;
import com.zetcom.mpria.config.domain.RevisionStatus;
import com.zetcom.mpria.config.infrastructure.memory.InMemoryConfigurationRepository;
import com.zetcom.mpria.config.testing.ConfigFixtures;
import com.zetcom.mpria.pm.GridSpec;
import com.zetcom.mpria.pm.Layout;
import com.zetcom.mpria.pm.LayoutNode;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** The write side: editing a draft, and the gate between that draft and everybody else. */
class ConfigurationDraftServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-01T12:00:00Z");
    private static final String DRAFT = InMemoryConfigurationRepository.SEED_DRAFT_ID;

    private final InMemoryConfigurationRepository repository =
            new InMemoryConfigurationRepository(ConfigFixtures.publishedRevision());
    private final ConfigurationDraftService service = new ConfigurationDraftService(
            repository,
            new ConfigurationValidator(ComponentRegistry.standard()),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Nested
    class ReadingADraft {

        @Test
        void returns_the_draft_the_seed_opened_from_the_published_revision() {
            ConfigurationRevision draft = service.draft(DRAFT);

            assertThat(draft.status()).isEqualTo(RevisionStatus.DRAFT);
            assertThat(draft.screens()).isEqualTo(ConfigFixtures.publishedRevision().screens());
        }

        @Test
        void reports_an_unknown_draft() {
            assertThatExceptionOfType(DraftNotFoundException.class)
                    .isThrownBy(() -> service.draft("draft-nope"))
                    .withMessageContaining("draft-nope");
        }

        @Test
        void refuses_to_treat_a_published_revision_as_a_draft() {
            // Nothing at runtime may edit what consumers have already cached.
            assertThatExceptionOfType(DraftNotFoundException.class)
                    .isThrownBy(() -> service.draft("rev-1"));
        }
    }

    @Nested
    class ReplacingALayout {

        @Test
        void stores_the_new_layout_and_advances_the_etag() {
            ConfigurationRevision updated = service.replaceLayout(
                    DRAFT, "object-detail", emptyObjectDetailLayout(), "\"0\"");

            assertThat(updated.etag()).isEqualTo("\"1\"");
            assertThat(service.draft(DRAFT).screen("object-detail").orElseThrow().boundFieldIds())
                    .isEmpty();
        }

        @Test
        void accepts_a_draft_that_is_temporarily_invalid_because_the_gate_is_at_publish_time() {
            Layout danglingBinding = layoutBinding("provenance");

            service.replaceLayout(DRAFT, "object-detail", danglingBinding, null);

            assertThat(service.draft(DRAFT).screen("object-detail").orElseThrow().boundFieldIds())
                    .containsExactly("provenance");
        }

        @Test
        void rejects_a_write_built_against_a_stale_copy() {
            service.replaceLayout(DRAFT, "object-detail", emptyObjectDetailLayout(), "\"0\"");

            assertThatExceptionOfType(StaleDraftException.class)
                    .isThrownBy(() -> service.replaceLayout(
                            DRAFT, "object-detail", emptyObjectDetailLayout(), "\"0\""))
                    .withMessageContaining("\"0\"");
        }

        @Test
        void rejects_a_layout_that_names_a_different_screen_than_the_path_does() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> service.replaceLayout(
                            DRAFT, "object-list", emptyObjectDetailLayout(), null))
                    .withMessageContaining("object-list");
        }
    }

    @Nested
    class Publishing {

        @Test
        void emits_a_new_immutable_revision_that_becomes_the_current_one() {
            ConfigurationRevision published = service.publish(DRAFT);

            assertThat(published.status()).isEqualTo(RevisionStatus.PUBLISHED);
            assertThat(published.publishedAt()).isEqualTo(NOW);
            assertThat(published.id()).startsWith("rev-").isNotEqualTo("rev-1");
            assertThat(repository.currentPublished()).contains(published);
        }

        @Test
        void records_that_the_draft_passed_the_gate() {
            service.publish(DRAFT);

            assertThat(service.draft(DRAFT).status()).isEqualTo(RevisionStatus.VALIDATED);
        }

        @Test
        void refuses_a_draft_binding_a_field_its_record_type_does_not_define() {
            service.replaceLayout(DRAFT, "object-detail", layoutBinding("provenance"), null);

            assertThatExceptionOfType(ConfigurationValidationException.class)
                    .isThrownBy(() -> service.publish(DRAFT))
                    .satisfies(e -> assertThat(e.errors()).singleElement()
                            .satisfies(error -> assertThat(error.message())
                                    .contains("binds field 'provenance'")));
        }

        @Test
        void leaves_the_current_revision_alone_when_the_gate_refuses() {
            service.replaceLayout(DRAFT, "object-detail", layoutBinding("provenance"), null);

            assertThatExceptionOfType(ConfigurationValidationException.class)
                    .isThrownBy(() -> service.publish(DRAFT));

            assertThat(repository.currentPublished().orElseThrow().id()).isEqualTo("rev-1");
        }
    }

    @Nested
    class Exporting {

        @Test
        void bundles_a_revision_by_id() {
            ConfigurationBundle bundle = ConfigurationBundle.of(service.revision("rev-1"));

            assertThat(bundle.revisionId()).isEqualTo("rev-1");
            assertThat(bundle.recordTypes()).singleElement()
                    .satisfies(type -> assertThat(type.recordType()).isEqualTo("object"));
        }

        @Test
        void reports_an_unknown_revision() {
            assertThatExceptionOfType(RevisionNotFoundException.class)
                    .isThrownBy(() -> service.revision("rev-404"))
                    .withMessageContaining("rev-404");
        }
    }

    @Nested
    class Importing {

        private final ConfigurationBundle bundle =
                ConfigurationBundle.of(ConfigFixtures.publishedRevision());

        @Test
        void a_dry_run_writes_nothing_and_still_reports_the_verdict() {
            ImportReport report = service.importBundle(bundle, true);

            assertThat(report.valid()).isTrue();
            assertThat(report.draftId()).isNull();
            assertThat(service.draft(DRAFT).version()).isZero();
        }

        @Test
        void reports_the_effective_diff_against_what_is_published_today() {
            ImportReport report = service.importBundle(bundle, true);

            assertThat(report.diff()).isEqualTo(ConfigurationDiff.unchanged());
        }

        @Test
        void reports_an_added_screen_as_a_difference() {
            ConfigurationBundle extended = new ConfigurationBundle(
                    "rev-import", ConfigFixtures.PUBLISHED_AT, bundle.recordTypes(),
                    List.of(bundle.screens().getFirst(),
                            new ConfigurationBundle.ScreenBundle("object-list", "object",
                                    emptyLayout("object-list"))));

            assertThat(service.importBundle(extended, true).diff().screensAdded())
                    .containsExactly("object-list");
        }

        @Test
        void a_real_import_opens_a_new_draft_rather_than_publishing_anything() {
            ImportReport report = service.importBundle(bundle, false);

            assertThat(report.draftId()).isNotNull();
            assertThat(service.draft(report.draftId()).status()).isEqualTo(RevisionStatus.DRAFT);
            assertThat(repository.currentPublished().orElseThrow().id()).isEqualTo("rev-1");
        }

        @Test
        void a_dry_run_of_an_invalid_bundle_reports_rather_than_throws() {
            ImportReport report = service.importBundle(invalidBundle(), true);

            assertThat(report.valid()).isFalse();
            assertThat(report.errors()).singleElement()
                    .satisfies(error -> assertThat(error.message()).contains("provenance"));
        }

        @Test
        void a_real_import_of_an_invalid_bundle_is_refused_outright() {
            assertThatExceptionOfType(ConfigurationValidationException.class)
                    .isThrownBy(() -> service.importBundle(invalidBundle(), false));
        }

        private ConfigurationBundle invalidBundle() {
            return new ConfigurationBundle("rev-import", ConfigFixtures.PUBLISHED_AT,
                    bundle.recordTypes(),
                    List.of(new ConfigurationBundle.ScreenBundle(
                            "object-detail", "object", layoutBinding("provenance"))));
        }
    }

    private static Layout emptyObjectDetailLayout() {
        return emptyLayout("object-detail");
    }

    private static Layout emptyLayout(String screenId) {
        return new Layout(screenId, "object",
                List.of(new LayoutNode.Region("main", GridSpec.singleColumn(), List.of())));
    }

    private static Layout layoutBinding(String fieldId) {
        return new Layout("object-detail", "object", List.of(new LayoutNode.Region(
                "main", GridSpec.singleColumn(),
                List.of(ConfigFixtures.slot("slot-x", "TEXT_FIELD", fieldId, null)))));
    }
}
