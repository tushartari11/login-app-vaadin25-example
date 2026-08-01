package com.zetcom.mpria.config.domain;

import java.time.Instant;
import java.util.List;

import com.zetcom.mpria.config.testing.ConfigFixtures;
import com.zetcom.mpria.pm.GridSpec;
import com.zetcom.mpria.pm.Layout;
import com.zetcom.mpria.pm.LayoutNode;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * A revision is immutable, and every transition returns a new one. That is what makes rollback a
 * pointer move rather than a restore (docs/ui-configuration.md §2.1) and what lets a consumer cache
 * a fetched revision for ever.
 */
class ConfigurationRevisionTest {

    private static final Instant AT = Instant.parse("2026-08-01T10:15:30Z");

    @Nested
    class Construction {

        @Test
        void starts_a_draft_unpublished_and_at_version_zero() {
            ConfigurationRevision draft = ConfigFixtures.draftRevision();

            assertThat(draft.status()).isEqualTo(RevisionStatus.DRAFT);
            assertThat(draft.publishedAt()).isNull();
            assertThat(draft.etag()).isEqualTo("\"0\"");
        }

        @Test
        void rejects_a_published_revision_with_no_publication_timestamp() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ConfigurationRevision("rev-1", RevisionStatus.PUBLISHED, null,
                            0, List.of(ConfigFixtures.objectRecordType()), List.of()))
                    .withMessageContaining("publishedAt");
        }

        @Test
        void rejects_a_publication_timestamp_on_something_not_yet_published() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ConfigurationRevision("draft-1", RevisionStatus.DRAFT, AT,
                            0, List.of(ConfigFixtures.objectRecordType()), List.of()))
                    .withMessageContaining("publishedAt");
        }

        @Test
        void rejects_two_definitions_of_the_same_record_type() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ConfigurationRevision.draft("draft-1",
                            List.of(ConfigFixtures.objectRecordType(), ConfigFixtures.objectRecordType()),
                            List.of()))
                    .withMessageContaining("duplicate record type 'object'");
        }

        @Test
        void rejects_two_layouts_for_the_same_screen() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ConfigurationRevision.draft("draft-1",
                            List.of(ConfigFixtures.objectRecordType()),
                            List.of(ConfigFixtures.objectDetailScreen(), ConfigFixtures.objectDetailScreen())))
                    .withMessageContaining("duplicate screen 'object-detail'");
        }
    }

    @Nested
    class Lookup {

        private final ConfigurationRevision revision = ConfigFixtures.publishedRevision();

        @Test
        void finds_a_record_type_and_a_screen_by_id() {
            assertThat(revision.recordType("object")).isPresent();
            assertThat(revision.screen("object-detail")).isPresent();
        }

        @Test
        void reports_an_unconfigured_id_as_absent_rather_than_guessing() {
            assertThat(revision.recordType("acquisition")).isEmpty();
            assertThat(revision.screen("object-list")).isEmpty();
        }
    }

    @Nested
    class Editing {

        @Test
        void replacing_a_layout_leaves_the_original_revision_untouched() {
            ConfigurationRevision draft = ConfigFixtures.draftRevision();

            ConfigurationRevision edited = draft.withScreen(emptyObjectDetail());

            assertThat(draft.screen("object-detail").orElseThrow().layout().boundFieldIds()).isNotEmpty();
            assertThat(edited.screen("object-detail").orElseThrow().layout().boundFieldIds()).isEmpty();
        }

        @Test
        void replacing_a_layout_advances_the_etag_so_a_concurrent_editor_is_caught() {
            ConfigurationRevision edited = ConfigFixtures.draftRevision().withScreen(emptyObjectDetail());

            assertThat(edited.etag()).isEqualTo("\"1\"");
        }

        @Test
        void adds_a_screen_the_draft_did_not_have_yet() {
            ConfigurationRevision draft = ConfigurationRevision.draft(
                    "draft-1", List.of(ConfigFixtures.objectRecordType()), List.of());

            assertThat(draft.withScreen(ConfigFixtures.objectDetailScreen()).screens()).hasSize(1);
        }

        @Test
        void returns_a_validated_candidate_to_draft_because_the_verdict_no_longer_describes_it() {
            ConfigurationRevision candidate = ConfigFixtures.draftRevision().validated();

            assertThat(candidate.status()).isEqualTo(RevisionStatus.VALIDATED);
            assertThat(candidate.withScreen(emptyObjectDetail()).status()).isEqualTo(RevisionStatus.DRAFT);
        }

        @Test
        void refuses_to_edit_a_published_revision() {
            assertThatIllegalStateException()
                    .isThrownBy(() -> ConfigFixtures.publishedRevision().withScreen(emptyObjectDetail()))
                    .withMessageContaining("published revision 'rev-1' is immutable");
        }
    }

    @Nested
    class Publishing {

        @Test
        void emits_a_new_immutable_revision_and_keeps_the_content() {
            ConfigurationRevision published = ConfigFixtures.draftRevision().validated().publishAs("rev-2", AT);

            assertThat(published.id()).isEqualTo("rev-2");
            assertThat(published.status()).isEqualTo(RevisionStatus.PUBLISHED);
            assertThat(published.publishedAt()).isEqualTo(AT);
            assertThat(published.screens()).isEqualTo(ConfigFixtures.draftRevision().screens());
        }

        @Test
        void refuses_to_publish_a_candidate_that_was_never_validated() {
            assertThatIllegalStateException()
                    .isThrownBy(() -> ConfigFixtures.draftRevision().publishAs("rev-2", AT))
                    .withMessageContaining("must be VALIDATED before publication");
        }

        @Test
        void reopens_a_published_revision_as_a_draft_for_further_editing() {
            ConfigurationRevision draft = ConfigFixtures.publishedRevision().asDraft("draft-9");

            assertThat(draft.id()).isEqualTo("draft-9");
            assertThat(draft.status()).isEqualTo(RevisionStatus.DRAFT);
            assertThat(draft.publishedAt()).isNull();
            assertThat(draft.etag()).isEqualTo("\"0\"");
        }
    }

    private static ScreenLayout emptyObjectDetail() {
        return new ScreenLayout("object-detail", "object", new Layout("object-detail", "object",
                List.of(new LayoutNode.Region("main", GridSpec.singleColumn(), List.of()))));
    }
}
