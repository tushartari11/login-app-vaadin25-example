package com.zetcom.mpria.config.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One entry of the component registry: something an administrator can drag onto the canvas and
 * every renderer is expected to draw.
 *
 * @param label             a literal caption for the palette. The admin console is an internal
 *                          tool with one audience, so unlike a configured field's label this is
 *                          not an i18n key
 * @param acceptsFieldTypes the field types this component can draw. Empty means the component
 *                          draws no field at all, such as a media gallery driven by a relation.
 *                          This is what makes drag-and-drop safe: the designer refuses an
 *                          incompatible drop before an invalid configuration can exist
 * @param options           option name → its declared type and default
 */
public record ComponentDefinition(String type,
                                  String label,
                                  List<FieldType> acceptsFieldTypes,
                                  Map<String, OptionSpec> options) {

    public ComponentDefinition {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("component type must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("component '" + type + "' has no label");
        }
        acceptsFieldTypes = acceptsFieldTypes == null ? List.of() : List.copyOf(acceptsFieldTypes);
        // Not Map.copyOf: that returns an unordered map, and the palette's inspector shows options
        // in declaration order.
        options = options == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(options));
    }

    public boolean accepts(FieldType fieldType) {
        return acceptsFieldTypes.contains(fieldType);
    }

    /** A component that can draw a field must be given one; an empty slot renders as nothing. */
    public boolean requiresFieldBinding() {
        return !acceptsFieldTypes.isEmpty();
    }

    /**
     * The declared type and default of one component option.
     *
     * @param type         a JSON type name — {@code boolean}, {@code integer}, {@code string}
     * @param defaultValue always present, so a renderer never has to invent one and two renderers
     *                     cannot invent different ones
     */
    public record OptionSpec(String type, Object defaultValue) {

        public OptionSpec {
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("component option has no type");
            }
            if (defaultValue == null) {
                throw new IllegalArgumentException("component option of type " + type + " has no default");
            }
        }
    }
}
