package com.zetcom.mpria.config.infrastructure.seed;

import java.util.List;

import com.zetcom.mpria.config.domain.ComponentRegistry;
import com.zetcom.mpria.config.domain.ConfigurationRevision;
import com.zetcom.mpria.config.domain.ConfigurationValidator;
import com.zetcom.mpria.config.domain.FieldDefinition;
import com.zetcom.mpria.config.domain.FieldType;
import com.zetcom.mpria.config.domain.RecordTypeDefinition;
import com.zetcom.mpria.config.domain.RevisionStatus;
import com.zetcom.mpria.config.domain.ScreenLayout;
import com.zetcom.mpria.config.testing.ConfigFixtures;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seed is the whole demonstration.
 *
 * <p>Every other service in the walking skeleton reads its record types and its layout from this
 * one file, so a typo in it does not fail here — it fails as an empty form three services away.
 * These tests are the reason that cannot happen quietly.
 */
class SeedRevisionReaderTest {

    private final ConfigurationRevision seed =
            new SeedRevisionReader(JsonMapper.builder().build()).read();

    @Test
    void loads_as_the_published_revision_the_contract_names() {
        assertThat(seed.id()).isEqualTo("rev-1");
        assertThat(seed.status()).isEqualTo(RevisionStatus.PUBLISHED);
        assertThat(seed.publishedAt()).isEqualTo(ConfigFixtures.PUBLISHED_AT);
    }

    @Nested
    class TheObjectRecordType {

        private final RecordTypeDefinition object = seed.recordType("object").orElseThrow();

        @Test
        void defines_exactly_the_fields_docs_contracts_md_fixes() {
            assertThat(object.schemaVersion()).isEqualTo(1);
            assertThat(object.titleFieldId()).isEqualTo("title");
            assertThat(object.fields()).containsExactlyElementsOf(
                    ConfigFixtures.objectRecordType().fields());
        }

        @Test
        void constrains_the_inventory_number_as_the_contract_says() {
            FieldDefinition inventoryNumber = object.field("inventoryNumber").orElseThrow();

            assertThat(inventoryNumber.type()).isEqualTo(FieldType.STRING);
            assertThat(inventoryNumber.required()).isTrue();
            assertThat(inventoryNumber.maxLength()).isEqualTo(50);
        }

        @Test
        void offers_the_condition_values_in_the_order_the_contract_lists_them() {
            assertThat(object.field("condition").orElseThrow().allowedValues())
                    .containsExactly("GOOD", "FAIR", "POOR");
        }
    }

    @Nested
    class TheObjectDetailScreen {

        private final ScreenLayout screen = seed.screen("object-detail").orElseThrow();

        /**
         * The drift guard. If the seed ever binds a field the record type dropped, or stops binding
         * one it gained, the end-to-end demo renders a form with a hole in it — and this is the
         * only place that catches it before a browser does.
         */
        @Test
        void binds_exactly_the_field_ids_the_record_type_defines() {
            List<String> defined = seed.recordType("object").orElseThrow().fieldIds();

            assertThat(screen.boundFieldIds()).containsExactlyInAnyOrderElementsOf(defined);
        }

        @Test
        void has_the_two_group_responsive_shape_the_contract_illustrates() {
            assertThat(screen.layout()).isEqualTo(ConfigFixtures.objectDetailLayout());
        }
    }

    @Test
    void passes_the_same_publish_gate_a_human_edited_draft_has_to_pass() {
        assertThat(new ConfigurationValidator(ComponentRegistry.standard()).validate(seed)).isEmpty();
    }

    @Test
    void is_the_resource_the_assignment_names_and_not_some_other_file() {
        assertThat(new ClassPathResource(SeedRevisionReader.SEED_RESOURCE).exists()).isTrue();
    }
}
