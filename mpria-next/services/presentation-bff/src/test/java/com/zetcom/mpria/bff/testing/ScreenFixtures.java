package com.zetcom.mpria.bff.testing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.zetcom.mpria.bff.domain.FieldDefinition;
import com.zetcom.mpria.bff.domain.Lifecycle;
import com.zetcom.mpria.bff.domain.RecordSchema;
import com.zetcom.mpria.bff.domain.RecordSnapshot;
import com.zetcom.mpria.bff.domain.ScreenDefinition;
import com.zetcom.mpria.pm.ComponentRef;
import com.zetcom.mpria.pm.GridSpec;
import com.zetcom.mpria.pm.Layout;
import com.zetcom.mpria.pm.LayoutNode;

/**
 * A customer's configuration, as test data.
 *
 * <p>{@code inventoryNumber}, {@code condition} and {@code object} are a <em>museum's</em>
 * vocabulary, and they appear here and nowhere in {@code src/main}. That is the property these
 * fixtures exist to keep honest: the framework knows record types and fields, never paintings
 * (ARCHITECTURE.md §1.3).
 */
public final class ScreenFixtures {

    public static final String SCREEN_ID = "object-detail";
    public static final String RECORD_TYPE = "object";
    public static final String REVISION_ID = "rev-1";
    public static final String ETAG = "\"7\"";
    public static final UUID RECORD_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    public static final UUID TENANT_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");

    private ScreenFixtures() {
    }

    /** A two-group detail screen. The group span is asserted verbatim by the JSON contract test. */
    public static ScreenDefinition screen() {
        return new ScreenDefinition(SCREEN_ID, RECORD_TYPE, layout());
    }

    public static Layout layout() {
        LayoutNode.Group identification = new LayoutNode.Group(
                "grp-identification",
                "config.group.identification",
                true,
                Map.of("base", 1, "md", 6, "xl", 8),
                GridSpec.of(Map.of("base", 1, "md", 2)),
                List.of(
                        slot("slot-inv", "inventoryNumber"),
                        slot("slot-title", "title")));

        LayoutNode.Group condition = new LayoutNode.Group(
                "grp-condition",
                "config.group.condition",
                false,
                Map.of("base", 1, "md", 6, "xl", 4),
                GridSpec.singleColumn(),
                List.of(slot("slot-condition", "condition")));

        LayoutNode.Region main = new LayoutNode.Region(
                "main",
                GridSpec.of(Map.of("base", 1, "md", 6, "xl", 12)),
                List.of(identification, condition));

        return new Layout(SCREEN_ID, RECORD_TYPE, List.of(main));
    }

    /** A layout binding a field id the schema below does not define. */
    public static ScreenDefinition screenBindingAnUnknownField() {
        LayoutNode.Region main = new LayoutNode.Region(
                "main",
                GridSpec.singleColumn(),
                List.of(slot("slot-colour", "dominantColour")));

        return new ScreenDefinition(SCREEN_ID, RECORD_TYPE, new Layout(SCREEN_ID, RECORD_TYPE, List.of(main)));
    }

    public static RecordSchema schema() {
        return new RecordSchema(RECORD_TYPE, 1, "title", List.of(
                new FieldDefinition("inventoryNumber", "STRING", "config.field.inventoryNumber",
                        true, List.of()),
                new FieldDefinition("title", "STRING", "config.field.title",
                        false, List.of()),
                new FieldDefinition("condition", "ENUM", "config.field.condition",
                        false, List.of("GOOD", "FAIR", "POOR"))));
    }

    public static RecordSnapshot record(Lifecycle lifecycle) {
        return new RecordSnapshot(RECORD_ID, RECORD_TYPE, 1, "INV-0001", "Study of a hand",
                lifecycle, values(), ETAG);
    }

    public static Map<String, Object> values() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("inventoryNumber", "INV-0001");
        data.put("title", "Study of a hand");
        data.put("condition", "GOOD");
        return data;
    }

    private static LayoutNode.Slot slot(String slotId, String fieldId) {
        return new LayoutNode.Slot(slotId, Map.of("base", 1), ComponentRef.bound("TEXT_FIELD", fieldId));
    }
}
