package com.zetcom.mpria.pm;

import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The contract is only a contract if it survives the wire. These tests pin the JSON shape that
 * generated TypeScript types and every non-Java client depend on.
 */
class PresentationModelJsonTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private final PresentationModel model = new PresentationModel(
            new ScreenMeta("object-detail", "rev-1", "rec-1", "\"7\"", List.of("EDIT", "DELETE")),
            LayoutFixtures.twoGroupScreen(),
            LayoutFixtures.fieldStatesFor(LayoutFixtures.twoGroupScreen()),
            List.of(ActionModel.enabled("save", "action.save", "PUT", "/api/v1/records/rec-1")));

    @Test
    void survives_a_round_trip_unchanged() {
        String json = mapper.writeValueAsString(model);
        PresentationModel restored = mapper.readValue(json, PresentationModel.class);

        assertThat(restored).isEqualTo(model);
    }

    @Test
    void discriminates_layout_nodes_by_kind_so_any_client_can_dispatch() {
        String json = mapper.writeValueAsString(model);

        assertThat(json)
                .contains("\"kind\":\"REGION\"")
                .contains("\"kind\":\"GROUP\"")
                .contains("\"kind\":\"SLOT\"");
    }

    @Test
    void omits_nulls_so_the_payload_stays_small_on_wide_screens() {
        String json = mapper.writeValueAsString(
                new LayoutNode.Slot("s", null, ComponentRef.unbound("MEDIA_GALLERY")));

        assertThat(json).doesNotContain("fieldId");
    }

    @Test
    void breakpoint_spans_serialise_as_plain_named_keys() {
        String json = mapper.writeValueAsString(model.layout());

        assertThat(json).contains("\"span\":{\"base\":1,\"md\":6,\"xl\":8}");
    }
}
