package com.zetcom.mpria.pm;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LayoutTest {

    @Test
    void collects_bound_field_ids_in_document_order() {
        Layout layout = LayoutFixtures.twoGroupScreen();

        assertThat(layout.boundFieldIds())
                .containsExactly("inventoryNumber", "title", "description", "acquiredOn");
    }

    @Test
    void ignores_unbound_components_such_as_a_media_gallery() {
        Layout layout = new Layout("s", "object", List.of(
                new LayoutNode.Region("main", GridSpec.singleColumn(), List.of(
                        new LayoutNode.Slot("slot-gallery", Map.of("base", 1),
                                ComponentRef.unbound("MEDIA_GALLERY"))))));

        assertThat(layout.boundFieldIds()).isEmpty();
    }

    @Test
    void rejects_duplicate_node_ids_anywhere_in_the_tree() {
        List<LayoutNode> regions = List.of(
                new LayoutNode.Region("main", GridSpec.singleColumn(), List.of(
                        new LayoutNode.Slot("clash", Map.of("base", 1),
                                ComponentRef.bound("TEXT_FIELD", "a")))),
                new LayoutNode.Region("side", GridSpec.singleColumn(), List.of(
                        new LayoutNode.Slot("clash", Map.of("base", 1),
                                ComponentRef.bound("TEXT_FIELD", "b")))));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Layout("s", "object", regions))
                .withMessageContaining("duplicate layout node id 'clash'");
    }

    @Test
    void walks_depth_first_in_document_order() {
        List<String> visited = new java.util.ArrayList<>();
        LayoutFixtures.twoGroupScreen().walk(node -> visited.add(node.id()));

        assertThat(visited).containsExactly(
                "main",
                "grp-identification", "slot-inv", "slot-title", "slot-desc",
                "grp-acquisition", "slot-acquired");
    }

    @Test
    void a_slot_without_a_component_is_not_constructible() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LayoutNode.Slot("orphan", Map.of("base", 1), null))
                .withMessageContaining("orphan");
    }
}
