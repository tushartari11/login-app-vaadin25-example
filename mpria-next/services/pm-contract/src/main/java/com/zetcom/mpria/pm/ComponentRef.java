package com.zetcom.mpria.pm;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * What a {@link LayoutNode.Slot} draws.
 *
 * <p>{@code type} is a key into the client's component registry — the single point of coupling
 * between configuration and any given renderer. A renderer that implements the registry can draw
 * every screen the platform will ever be configured to produce.
 *
 * @param type      registry key, e.g. {@code TEXT_FIELD}, {@code RICH_TEXT}, {@code MEDIA_GALLERY}
 * @param fieldId   the field this component is bound to, or {@code null} for data-less components
 * @param options   component-specific settings declared by the registry entry
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComponentRef(String type, String fieldId, Map<String, Object> options) {

    public ComponentRef {
        type = Contract.requireId(type, "component type");
        options = options == null ? Map.of() : Map.copyOf(options);
    }

    public static ComponentRef bound(String type, String fieldId) {
        return new ComponentRef(type, Contract.requireId(fieldId, "fieldId"), Map.of());
    }

    public static ComponentRef unbound(String type) {
        return new ComponentRef(type, null, Map.of());
    }

    /** Convenience for Java callers. Not part of the wire contract — {@code fieldId} already says this. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isBound() {
        return fieldId != null;
    }
}
