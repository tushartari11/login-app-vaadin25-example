package com.zetcom.mpria.config.domain;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * A record type is what {@code catalog-service} compiles into its payload validator, so a
 * definition that is internally inconsistent must never leave this service.
 */
class RecordTypeDefinitionTest {

    @Test
    void exposes_its_field_ids_in_configured_order() {
        assertThat(fixture().fieldIds())
                .containsExactly("inventoryNumber", "title");
    }

    @Test
    void finds_a_field_by_id() {
        assertThat(fixture().field("title")).map(FieldDefinition::type).contains(FieldType.STRING);
        assertThat(fixture().field("absent")).isEmpty();
    }

    @Test
    void rejects_duplicate_field_ids() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RecordTypeDefinition("object", 1, null, List.of(
                        string("title"), string("title"))))
                .withMessageContaining("duplicate field id 'title'");
    }

    @Test
    void rejects_a_title_field_that_is_not_one_of_its_fields() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RecordTypeDefinition("object", 1, "absent", List.of(string("title"))))
                .withMessageContaining("titleFieldId 'absent'");
    }

    @Test
    void rejects_a_schema_version_below_one() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RecordTypeDefinition("object", 0, null, List.of(string("title"))))
                .withMessageContaining("schemaVersion");
    }

    @Test
    void rejects_a_record_type_with_no_fields_at_all() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RecordTypeDefinition("object", 1, null, List.of()))
                .withMessageContaining("no fields");
    }

    private static RecordTypeDefinition fixture() {
        return new RecordTypeDefinition("object", 1, "title",
                List.of(string("inventoryNumber"), string("title")));
    }

    private static FieldDefinition string(String id) {
        return new FieldDefinition(id, FieldType.STRING, null, false, null, List.of());
    }
}
