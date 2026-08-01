package com.zetcom.mpria.pm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PresentationModelTest {

    private final Layout layout = LayoutFixtures.twoGroupScreen();
    private final ScreenMeta meta =
            new ScreenMeta("object-detail", "rev-1", "rec-1", "\"7\"", List.of("EDIT"));

    @Test
    void a_renderer_is_never_handed_a_slot_it_has_no_field_state_for() {
        Map<String, FieldState> incomplete = new HashMap<>(LayoutFixtures.fieldStatesFor(layout));
        incomplete.remove("title");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PresentationModel(meta, layout, incomplete, List.of()))
                .withMessageContaining("binds field(s) with no state")
                .withMessageContaining("title");
    }

    @Test
    void accepts_a_complete_model() {
        PresentationModel pm = new PresentationModel(
                meta, layout, LayoutFixtures.fieldStatesFor(layout),
                List.of(ActionModel.enabled("save", "action.save", "PUT", "/api/v1/records/rec-1")));

        assertThat(pm.fields()).containsOnlyKeys("inventoryNumber", "title", "description", "acquiredOn");
        assertThat(pm.actions()).singleElement()
                .satisfies(action -> assertThat(action.enabled()).isTrue());
    }

    @Test
    void a_disabled_action_must_explain_itself_rather_than_render_an_inert_button() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ActionModel("delete", "action.delete", "DELETE", "/x", false, null, false))
                .withMessageContaining("must state a reason");

        assertThat(ActionModel.disabled("delete", "action.delete", "DELETE", "/x", "record.published")
                .disabledReason()).isEqualTo("record.published");
    }

    @Test
    void field_state_transformers_are_side_effect_free() {
        FieldState original = FieldState.editable("STRING", "config.field.title", "Mona Lisa");
        FieldState readOnly = original.asReadOnly().withErrors(List.of("validation.tooLong"));

        assertThat(original.readOnly()).isFalse();
        assertThat(original.errors()).isEmpty();
        assertThat(readOnly.readOnly()).isTrue();
        assertThat(readOnly.errors()).containsExactly("validation.tooLong");
        assertThat(readOnly.value()).isEqualTo("Mona Lisa");
    }
}
