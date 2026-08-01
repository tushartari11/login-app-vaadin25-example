package com.zetcom.mpria.pm;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sample layouts for contract tests.
 *
 * <p>The field ids here ({@code inventoryNumber}, {@code title}, …) stand in for a <em>customer's
 * configuration</em>. Nothing in {@code src/main} knows these names exist — that is the property
 * these fixtures are here to keep honest.
 */
final class LayoutFixtures {

    private LayoutFixtures() {
    }

    /** A two-group detail screen: a wide identification block beside a narrow acquisition block. */
    static Layout twoGroupScreen() {
        LayoutNode.Group identification = new LayoutNode.Group(
                "grp-identification",
                "config.group.identification",
                true,
                Map.of("base", 1, "md", 6, "xl", 8),
                GridSpec.of(Map.of("base", 1, "md", 2)),
                List.of(
                        slot("slot-inv", "inventoryNumber", Map.of("base", 1)),
                        slot("slot-title", "title", Map.of("base", 1)),
                        slot("slot-desc", "description", Map.of("base", 1, "md", 2))));

        LayoutNode.Group acquisition = new LayoutNode.Group(
                "grp-acquisition",
                "config.group.acquisition",
                false,
                Map.of("base", 1, "md", 6, "xl", 4),
                GridSpec.singleColumn(),
                List.of(slot("slot-acquired", "acquiredOn", Map.of("base", 1))));

        LayoutNode.Region main = new LayoutNode.Region(
                "main",
                GridSpec.of(Map.of("base", 1, "md", 6, "xl", 12)),
                List.of(identification, acquisition));

        return new Layout("object-detail", "object", List.of(main));
    }

    /** An editable string state for every field the layout binds. */
    static Map<String, FieldState> fieldStatesFor(Layout layout) {
        return layout.boundFieldIds().stream().collect(Collectors.toMap(
                fieldId -> fieldId,
                fieldId -> FieldState.editable("STRING", "config.field." + fieldId, null)));
    }

    private static LayoutNode.Slot slot(String slotId, String fieldId, Map<String, Integer> span) {
        return new LayoutNode.Slot(slotId, span, ComponentRef.bound("TEXT_FIELD", fieldId));
    }
}
