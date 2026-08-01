package com.zetcom.mpria.config.domain;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class FieldDefinitionTest {

    @Test
    void defaults_the_label_to_a_derived_i18n_key_never_a_literal_caption() {
        assertThat(new FieldDefinition("title", FieldType.STRING, null, false, null, List.of()).label())
                .isEqualTo("config.field.title");
    }

    @Test
    void keeps_allowed_values_in_configured_order_because_that_is_the_order_a_list_offers_them() {
        FieldDefinition condition = new FieldDefinition(
                "condition", FieldType.ENUM, null, false, null, List.of("GOOD", "FAIR", "POOR"));

        assertThat(condition.allowedValues()).containsExactly("GOOD", "FAIR", "POOR");
    }

    @Test
    void rejects_an_enum_that_declares_no_allowed_values() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FieldDefinition("condition", FieldType.ENUM, null, false, null, List.of()))
                .withMessageContaining("declares no allowed values");
    }

    @Test
    void rejects_allowed_values_on_a_type_that_cannot_constrain_them() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FieldDefinition(
                        "title", FieldType.STRING, null, false, null, List.of("GOOD")))
                .withMessageContaining("only an ENUM field may declare allowed values");
    }

    @Test
    void rejects_duplicate_allowed_values() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FieldDefinition(
                        "condition", FieldType.ENUM, null, false, null, List.of("GOOD", "GOOD")))
                .withMessageContaining("duplicate allowed value 'GOOD'");
    }

    @Test
    void rejects_a_max_length_below_one() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FieldDefinition("title", FieldType.STRING, null, false, 0, List.of()))
                .withMessageContaining("maxLength");
    }

    @Test
    void rejects_a_blank_id() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new FieldDefinition(" ", FieldType.STRING, null, false, null, List.of()))
                .withMessageContaining("field id");
    }
}
