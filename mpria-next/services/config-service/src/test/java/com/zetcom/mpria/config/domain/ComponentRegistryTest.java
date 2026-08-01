package com.zetcom.mpria.config.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The registry is the contract between the palette the SME drags from and the components a
 * renderer implements.
 */
class ComponentRegistryTest {

    private final ComponentRegistry registry = ComponentRegistry.standard();

    @Test
    void offers_at_least_one_component_for_every_field_type() {
        // A field type with no component is a field an administrator can define but never place.
        for (FieldType type : FieldType.values()) {
            assertThat(registry.entries())
                    .as("a component accepting %s", type)
                    .anyMatch(component -> component.accepts(type));
        }
    }

    @Test
    void publishes_the_text_field_entry_exactly_as_the_contract_fixes_it() {
        ComponentDefinition textField = registry.find("TEXT_FIELD").orElseThrow();

        assertThat(textField.label()).isEqualTo("Text field");
        assertThat(textField.acceptsFieldTypes()).containsExactly(FieldType.STRING);
        assertThat(textField.options()).containsOnlyKeys("multiline");
        assertThat(textField.options().get("multiline"))
                .isEqualTo(new ComponentDefinition.OptionSpec("boolean", false));
    }

    @Test
    void publishes_a_component_that_draws_no_field_at_all() {
        ComponentDefinition gallery = registry.find("MEDIA_GALLERY").orElseThrow();

        assertThat(gallery.acceptsFieldTypes()).isEmpty();
        assertThat(gallery.requiresFieldBinding()).isFalse();
        assertThat(gallery.options()).isEmpty();
    }

    @Test
    void does_not_know_a_component_it_was_never_given() {
        assertThat(registry.find("CAROUSEL")).isEmpty();
    }

    @Test
    void declares_a_default_for_every_option_so_a_renderer_never_has_to_guess() {
        assertThat(registry.entries())
                .flatExtracting(component -> component.options().values())
                .allSatisfy(option -> assertThat(option.defaultValue()).isNotNull());
    }
}
