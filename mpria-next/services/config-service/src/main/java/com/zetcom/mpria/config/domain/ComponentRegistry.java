package com.zetcom.mpria.config.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.zetcom.mpria.config.domain.ComponentDefinition.OptionSpec;

/**
 * The components a screen may be built from.
 *
 * <p>Served to the admin console so the palette is never hardcoded there, and used by the publish
 * gate so a layout can only reference something a renderer actually implements. The two uses are
 * the same list on purpose: whatever the palette offers is exactly what publication accepts.
 *
 * <p>The registry is code rather than configuration because each entry is a contract with the
 * renderers. Adding one is a release of this service <em>and</em> of every client, so it cannot be
 * an administrator's decision.
 */
public final class ComponentRegistry {

    private final Map<String, ComponentDefinition> byType;

    private ComponentRegistry(List<ComponentDefinition> entries) {
        Map<String, ComponentDefinition> indexed = new LinkedHashMap<>();
        for (ComponentDefinition entry : entries) {
            if (indexed.put(entry.type(), entry) != null) {
                throw new IllegalArgumentException("duplicate component type '" + entry.type() + "'");
            }
        }
        // Not Map.copyOf: that returns an unordered map, and registry order is palette order.
        this.byType = Collections.unmodifiableMap(indexed);
    }

    /**
     * The components shipped with the product.
     *
     * <p>Every {@link FieldType} is drawable by at least one of them, which is the invariant that
     * keeps an administrator from defining a field that can be stored but never placed on a form.
     */
    public static ComponentRegistry standard() {
        return new ComponentRegistry(List.of(
                new ComponentDefinition("TEXT_FIELD", "Text field", List.of(FieldType.STRING),
                        Map.of("multiline", new OptionSpec("boolean", false))),
                new ComponentDefinition("RICH_TEXT", "Rich text", List.of(FieldType.TEXT),
                        Map.of("rows", new OptionSpec("integer", 6))),
                new ComponentDefinition("NUMBER_FIELD", "Number field",
                        List.of(FieldType.INTEGER, FieldType.DECIMAL), Map.of()),
                new ComponentDefinition("CHECKBOX", "Checkbox", List.of(FieldType.BOOLEAN), Map.of()),
                new ComponentDefinition("DATE_PICKER", "Date picker", List.of(FieldType.DATE), Map.of()),
                new ComponentDefinition("SELECT", "Select", List.of(FieldType.ENUM), Map.of()),
                new ComponentDefinition("MEDIA_GALLERY", "Media gallery", List.of(), Map.of())));
    }

    /** Registry order is palette order. */
    public List<ComponentDefinition> entries() {
        return List.copyOf(byType.values());
    }

    public Optional<ComponentDefinition> find(String type) {
        return Optional.ofNullable(byType.get(type));
    }
}
