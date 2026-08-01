package com.zetcom.mpria.bff.application;

import com.zetcom.mpria.bff.domain.Lifecycle;
import com.zetcom.mpria.bff.testing.FakeCatalogGateway;
import com.zetcom.mpria.bff.testing.FakeConfigurationGateway;
import com.zetcom.mpria.bff.testing.ScreenFixtures;
import com.zetcom.mpria.pm.PresentationModel;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static com.zetcom.mpria.bff.testing.ScreenFixtures.RECORD_ID;
import static com.zetcom.mpria.bff.testing.ScreenFixtures.RECORD_TYPE;
import static com.zetcom.mpria.bff.testing.ScreenFixtures.SCREEN_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the assembler emits, on the wire.
 *
 * <p>{@code pm-contract} already pins the shape of a hand-built model. This pins the shape of a
 * model this service actually assembles, because the document in {@code docs/CONTRACTS.md} is what
 * the generated TypeScript types and every non-Java client are written against.
 */
class PresentationModelJsonContractTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private final FakeConfigurationGateway configuration = new FakeConfigurationGateway()
            .with(ScreenFixtures.screen())
            .with(ScreenFixtures.schema());
    private final FakeCatalogGateway catalog =
            new FakeCatalogGateway().with(ScreenFixtures.record(Lifecycle.DRAFT));

    private final PresentationModelAssembler assembler =
            new PresentationModelAssembler(configuration, catalog);

    @Test
    void discriminates_layout_nodes_by_kind_so_any_client_can_dispatch() {
        String json = mapper.writeValueAsString(populated());

        assertThat(json)
                .contains("\"kind\":\"REGION\"")
                .contains("\"kind\":\"GROUP\"")
                .contains("\"kind\":\"SLOT\"");
    }

    @Test
    void breakpoint_spans_serialise_as_plain_named_keys_that_cascade_upward() {
        String json = mapper.writeValueAsString(populated().layout());

        assertThat(json).contains("\"span\":{\"base\":1,\"md\":6,\"xl\":8}");
    }

    @Test
    void omits_absent_keys_rather_than_writing_nulls() {
        String json = mapper.writeValueAsString(assembler.blankForm(SCREEN_ID, RECORD_TYPE));
        JsonNode model = mapper.readTree(json);

        assertThat(json).as("a blank form should carry no null literal at all").doesNotContain("null");
        assertThat(model.get("meta").propertyNames()).doesNotContain("recordId", "etag");
        assertThat(model.get("fields").properties()).allSatisfy(field ->
                assertThat(field.getValue().propertyNames()).doesNotContain("value"));
        assertThat(model.get("actions").get(0).propertyNames()).doesNotContain("disabledReason");
    }

    @Test
    void carries_the_four_parts_the_renderer_dispatches_on() {
        String json = mapper.writeValueAsString(populated());

        assertThat(mapper.readTree(json).propertyNames())
                .containsExactlyInAnyOrder("meta", "layout", "fields", "actions");
    }

    @Test
    void addresses_every_action_by_href_so_no_client_ever_builds_a_url() {
        String json = mapper.writeValueAsString(populated());

        assertThat(json)
                .contains("\"href\":\"/api/v1/screens/object-detail/" + RECORD_ID + "\"")
                .contains("\"href\":\"/api/v1/screens/object-detail/" + RECORD_ID
                        + "/lifecycle/PUBLISHED\"");
    }

    @Test
    void survives_a_round_trip_unchanged() {
        PresentationModel model = populated();

        String json = mapper.writeValueAsString(model);

        assertThat(mapper.readValue(json, PresentationModel.class)).isEqualTo(model);
    }

    private PresentationModel populated() {
        return assembler.existingRecord(SCREEN_ID, RECORD_ID);
    }
}
