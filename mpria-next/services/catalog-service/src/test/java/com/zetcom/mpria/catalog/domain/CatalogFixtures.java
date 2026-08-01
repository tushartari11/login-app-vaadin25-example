package com.zetcom.mpria.catalog.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A sample customer configuration and matching payloads.
 *
 * <p>These field names model what an administrator configured. Production code never mentions
 * them — that is the framework-neutrality rule from the project's context, kept honest by the fact
 * that these constants live only under {@code src/test}.
 */
public final class CatalogFixtures {

    public static final UUID TENANT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID OTHER_TENANT = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID ORG_UNIT = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final Instant NOW = Instant.parse("2026-08-01T10:15:30Z");

    private CatalogFixtures() {
    }

    public static RecordSchema objectSchema() {
        return new RecordSchema("object", 1, List.of(
                FieldDefinition.text("inventoryNumber").asRequired().withMaxLength(50),
                FieldDefinition.text("title").asRequired().withMaxLength(255),
                new FieldDefinition("description", FieldType.TEXT, null, false, null, Set.of()),
                new FieldDefinition("acquiredOn", FieldType.DATE, null, false, null, Set.of()),
                new FieldDefinition("condition", FieldType.ENUM, null, false, null,
                        Set.of("GOOD", "FAIR", "POOR"))),
                "title");
    }

    public static Map<String, Object> validPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("inventoryNumber", "INV-0001");
        payload.put("title", "Study of a hand");
        payload.put("description", "Red chalk on paper.");
        payload.put("acquiredOn", "1998-04-12");
        payload.put("condition", "GOOD");
        return payload;
    }

    public static CatalogRecord newRecord() {
        return CatalogRecord.create(UUID.randomUUID(), TENANT, ORG_UNIT, objectSchema(),
                "INV-0001", validPayload(), "curator@example.org", NOW);
    }
}
