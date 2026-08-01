package com.zetcom.mpria.config.testing;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.zetcom.mpria.config.domain.ConfigurationRevision;
import com.zetcom.mpria.config.domain.FieldDefinition;
import com.zetcom.mpria.config.domain.FieldType;
import com.zetcom.mpria.config.domain.RecordTypeDefinition;
import com.zetcom.mpria.config.domain.ScreenLayout;
import com.zetcom.mpria.pm.ComponentRef;
import com.zetcom.mpria.pm.GridSpec;
import com.zetcom.mpria.pm.Layout;
import com.zetcom.mpria.pm.LayoutNode;

/**
 * Sample configuration for the tests.
 *
 * <p>The record type and field ids here — {@code object}, {@code inventoryNumber}, … — stand in for
 * a <em>customer's configuration</em>. Nothing in {@code src/main} knows these names exist; they
 * reach the running service only as data, from {@code config/seed-revision.json}. Keeping them
 * confined to test fixtures is what these fixtures are for.
 */
public final class ConfigFixtures {

    /** Matches the seeded published revision, so a test may compare the two and catch drift. */
    public static final String REVISION_ID = "rev-1";

    public static final Instant PUBLISHED_AT = Instant.parse("2026-08-01T10:15:30Z");

    private ConfigFixtures() {
    }

    /** The {@code object} record type exactly as {@code docs/CONTRACTS.md} fixes it. */
    public static RecordTypeDefinition objectRecordType() {
        return new RecordTypeDefinition("object", 1, "title", List.of(
                new FieldDefinition("inventoryNumber", FieldType.STRING,
                        "config.field.inventoryNumber", true, 50, List.of()),
                new FieldDefinition("title", FieldType.STRING,
                        "config.field.title", true, 255, List.of()),
                new FieldDefinition("description", FieldType.TEXT,
                        "config.field.description", false, null, List.of()),
                new FieldDefinition("acquiredOn", FieldType.DATE,
                        "config.field.acquiredOn", false, null, List.of()),
                new FieldDefinition("condition", FieldType.ENUM,
                        "config.field.condition", false, null, List.of("GOOD", "FAIR", "POOR"))));
    }

    /** A two-group detail screen binding every field of {@link #objectRecordType()}. */
    public static ScreenLayout objectDetailScreen() {
        return new ScreenLayout("object-detail", "object", objectDetailLayout());
    }

    public static Layout objectDetailLayout() {
        LayoutNode.Group identification = new LayoutNode.Group(
                "grp-identification",
                "config.group.identification",
                true,
                Map.of("base", 1, "md", 6, "xl", 8),
                GridSpec.of(Map.of("base", 1, "md", 2)),
                List.of(
                        slot("slot-inv", "TEXT_FIELD", "inventoryNumber", Map.of("base", 1)),
                        slot("slot-title", "TEXT_FIELD", "title", Map.of("base", 1)),
                        slot("slot-desc", "RICH_TEXT", "description", Map.of("base", 1, "md", 2))));

        LayoutNode.Group acquisition = new LayoutNode.Group(
                "grp-acquisition",
                "config.group.acquisition",
                false,
                Map.of("base", 1, "md", 6, "xl", 4),
                GridSpec.singleColumn(),
                List.of(
                        slot("slot-acquired", "DATE_PICKER", "acquiredOn", Map.of("base", 1)),
                        slot("slot-condition", "SELECT", "condition", Map.of("base", 1))));

        LayoutNode.Region main = new LayoutNode.Region(
                "main",
                GridSpec.of(Map.of("base", 1, "md", 6, "xl", 12)),
                List.of(identification, acquisition));

        return new Layout("object-detail", "object", List.of(main));
    }

    /** A published revision equivalent to the seed, built in Java so tests need no resource. */
    public static ConfigurationRevision publishedRevision() {
        return ConfigurationRevision.published(
                REVISION_ID, PUBLISHED_AT, List.of(objectRecordType()), List.of(objectDetailScreen()));
    }

    public static ConfigurationRevision draftRevision() {
        return ConfigurationRevision.draft(
                "draft-1", List.of(objectRecordType()), List.of(objectDetailScreen()));
    }

    /** A single-slot screen, for tests that need to vary exactly one binding. */
    public static ScreenLayout screenBinding(String slotId, String componentType, String fieldId) {
        LayoutNode.Region main = new LayoutNode.Region(
                "main", GridSpec.singleColumn(),
                List.of(slot(slotId, componentType, fieldId, Map.of("base", 1))));
        return new ScreenLayout("object-detail", "object",
                new Layout("object-detail", "object", List.of(main)));
    }

    public static LayoutNode.Slot slot(String slotId,
                                       String componentType,
                                       String fieldId,
                                       Map<String, Integer> span) {
        ComponentRef component = fieldId == null
                ? ComponentRef.unbound(componentType)
                : ComponentRef.bound(componentType, fieldId);
        return new LayoutNode.Slot(slotId, span, component);
    }
}
