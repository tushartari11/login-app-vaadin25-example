package com.zetcom.mpria.catalog.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * The schema is what keeps the jsonb payload from being a free-for-all (ADR-0004). These tests are
 * the specification of "as constrained as the configuration says, and no more".
 */
class RecordSchemaTest {

    private final RecordSchema schema = CatalogFixtures.objectSchema();

    @Nested
    class Validation {

        @Test
        void accepts_a_payload_that_matches_the_configuration() {
            assertThat(schema.validate(CatalogFixtures.validPayload())).isEmpty();
        }

        @Test
        void reports_every_problem_at_once_rather_than_one_per_attempt() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", 42);                    // wrong type
            payload.put("acquiredOn", "not-a-date");     // unparseable
            payload.put("condition", "PRISTINE");        // not an allowed value
            // inventoryNumber omitted entirely -> required

            assertThat(schema.validate(payload)).containsOnlyKeys(
                    "inventoryNumber", "title", "acquiredOn", "condition");
        }

        @Test
        void rejects_keys_that_are_not_configured_fields() {
            Map<String, Object> payload = new LinkedHashMap<>(CatalogFixtures.validPayload());
            payload.put("smuggledField", "value");

            assertThat(schema.validate(payload))
                    .extractingByKey("smuggledField").asInstanceOf(
                            org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
                    .singleElement().asString().contains("is not a field of record type 'object'");
        }

        @Test
        void enforces_max_length_only_once_the_type_is_already_right() {
            RecordSchema shortTitle = new RecordSchema("object", 1,
                    List.of(FieldDefinition.text("title").withMaxLength(3)), "title");

            assertThat(shortTitle.validate(Map.of("title", "abcd")))
                    .extractingByKey("title").asInstanceOf(
                            org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
                    .containsExactly("must be at most 3 characters");

            // A wrong type reports the type problem alone; "3 characters" would be noise.
            assertThat(shortTitle.validate(Map.of("title", 99)))
                    .extractingByKey("title").asInstanceOf(
                            org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
                    .containsExactly("must be a string");
        }

        @Test
        void treats_a_missing_optional_field_as_valid() {
            Map<String, Object> minimal = Map.of("inventoryNumber", "INV-1", "title", "A title");

            assertThat(schema.validate(minimal)).isEmpty();
        }

        @Test
        void accepts_a_whole_number_arriving_as_any_json_number_type() {
            RecordSchema counts = new RecordSchema("object", 1, List.of(
                    new FieldDefinition("sheets", FieldType.INTEGER, null, false, null, Set.of())), null);

            assertThat(counts.validate(Map.of("sheets", 3))).isEmpty();
            assertThat(counts.validate(Map.of("sheets", 3L))).isEmpty();
            assertThat(counts.validate(Map.of("sheets", 3.0))).isEmpty();
            assertThat(counts.validate(Map.of("sheets", 3.5))).containsKey("sheets");
        }
    }

    @Nested
    class Normalisation {

        @Test
        void drops_nulls_and_orders_keys_by_configured_field_order() {
            Map<String, Object> unordered = new LinkedHashMap<>();
            unordered.put("condition", "GOOD");
            unordered.put("title", "A title");
            unordered.put("acquiredOn", null);
            unordered.put("inventoryNumber", "INV-1");

            assertThat(schema.normalise(unordered))
                    .containsExactly(
                            Map.entry("inventoryNumber", "INV-1"),
                            Map.entry("title", "A title"),
                            Map.entry("condition", "GOOD"));
        }

        @Test
        void derives_the_title_spine_column_from_the_configured_title_field() {
            assertThat(schema.titleOf(CatalogFixtures.validPayload())).contains("Study of a hand");
        }
    }

    @Nested
    class Construction {

        @Test
        void rejects_a_title_field_that_is_not_one_of_its_fields() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new RecordSchema("object", 1,
                            List.of(FieldDefinition.text("title")), "nonexistent"))
                    .withMessageContaining("nonexistent");
        }

        @Test
        void rejects_duplicate_field_ids() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new RecordSchema("object", 1,
                            List.of(FieldDefinition.text("title"), FieldDefinition.text("title")), null))
                    .withMessageContaining("duplicate field id");
        }

        @Test
        void rejects_an_enum_field_with_no_allowed_values() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new FieldDefinition(
                            "condition", FieldType.ENUM, null, false, null, Set.of()))
                    .withMessageContaining("declares no allowed values");
        }
    }
}
