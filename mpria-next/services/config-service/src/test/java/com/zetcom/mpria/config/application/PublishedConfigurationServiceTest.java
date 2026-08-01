package com.zetcom.mpria.config.application;

import java.util.Optional;

import com.zetcom.mpria.config.application.port.ConfigurationRepository;
import com.zetcom.mpria.config.domain.ComponentDefinition;
import com.zetcom.mpria.config.domain.ComponentRegistry;
import com.zetcom.mpria.config.domain.ConfigurationRevision;
import com.zetcom.mpria.config.infrastructure.memory.InMemoryConfigurationRepository;
import com.zetcom.mpria.config.testing.ConfigFixtures;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * The read side: what every other service resolves once and then caches for the lifetime of the
 * revision.
 */
class PublishedConfigurationServiceTest {

    private final PublishedConfigurationService service = new PublishedConfigurationService(
            new InMemoryConfigurationRepository(ConfigFixtures.publishedRevision()),
            ComponentRegistry.standard());

    @Test
    void resolves_the_current_revision() {
        assertThat(service.currentRevision().id()).isEqualTo("rev-1");
    }

    @Test
    void resolves_a_record_type_from_the_current_revision() {
        assertThat(service.recordType("object").titleFieldId()).isEqualTo("title");
    }

    @Test
    void reports_a_record_type_the_published_revision_does_not_define() {
        assertThatExceptionOfType(RecordTypeNotFoundException.class)
                .isThrownBy(() -> service.recordType("acquisition"))
                .withMessageContaining("acquisition");
    }

    @Test
    void returns_a_screen_together_with_the_revision_it_came_from() {
        PublishedScreen screen = service.screen("object-detail");

        // The BFF caches a layout by revision id, so a layout without one cannot be cached safely.
        assertThat(screen.revisionId()).isEqualTo("rev-1");
        assertThat(screen.layout().screenId()).isEqualTo("object-detail");
    }

    @Test
    void reports_a_screen_the_published_revision_does_not_configure() {
        assertThatExceptionOfType(ScreenNotFoundException.class)
                .isThrownBy(() -> service.screen("object-list"))
                .withMessageContaining("object-list");
    }

    @Test
    void serves_the_component_registry_so_the_palette_is_never_hardcoded() {
        assertThat(service.components())
                .extracting(ComponentDefinition::type)
                .contains("TEXT_FIELD", "MEDIA_GALLERY");
    }

    @Test
    void reports_that_nothing_is_published_rather_than_serving_an_empty_configuration() {
        PublishedConfigurationService nothingPublished =
                new PublishedConfigurationService(new EmptyStore(), ComponentRegistry.standard());

        assertThatExceptionOfType(RevisionNotFoundException.class)
                .isThrownBy(nothingPublished::currentRevision);
    }

    /** The state before the very first publish, which the seed means production never reaches. */
    private static final class EmptyStore implements ConfigurationRepository {

        @Override
        public Optional<ConfigurationRevision> currentPublished() {
            return Optional.empty();
        }

        @Override
        public Optional<ConfigurationRevision> findById(String revisionId) {
            return Optional.empty();
        }

        @Override
        public ConfigurationRevision save(ConfigurationRevision revision) {
            return revision;
        }

        @Override
        public void publish(ConfigurationRevision revision) {
            throw new UnsupportedOperationException();
        }
    }
}
