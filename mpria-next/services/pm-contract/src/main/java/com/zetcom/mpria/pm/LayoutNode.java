package com.zetcom.mpria.pm;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A node in a configured screen layout.
 *
 * <p>The hierarchy is intentionally shallow and closed: a {@link Region} is a grid container, a
 * {@link Group} is a labelled sub-grid inside it, and a {@link Slot} holds exactly one component.
 * Being {@code sealed} means every consumer that switches over a layout is checked by the compiler
 * for exhaustiveness — adding a node kind cannot silently skip a renderer or a validator.
 *
 * <p>No node carries a colour, a font or a pixel dimension. Configuration owns <em>structure</em>;
 * the design system owns <em>appearance</em>. Sizes are expressed only as column spans per named
 * breakpoint, which is what keeps configured screens responsive by construction.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LayoutNode.Region.class, name = "REGION"),
        @JsonSubTypes.Type(value = LayoutNode.Group.class, name = "GROUP"),
        @JsonSubTypes.Type(value = LayoutNode.Slot.class, name = "SLOT")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface LayoutNode {

    /** Stable identifier, unique within a screen. Referenced by overlay operations. */
    String id();

    /**
     * Discriminator for Java-side dispatch. Jackson writes the {@code kind} property itself from
     * the {@code @JsonTypeInfo} above, so this accessor is deliberately not serialised — were it
     * both, the property would appear twice on the wire.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    NodeKind kind();

    enum NodeKind { REGION, GROUP, SLOT }

    /**
     * A top-level grid container.
     *
     * @param grid column counts per breakpoint, e.g. {@code {"base":1,"md":6,"xl":12}}
     */
    record Region(String id, GridSpec grid, List<LayoutNode> children) implements LayoutNode {

        public Region {
            id = Contract.requireId(id, "region id");
            grid = grid == null ? GridSpec.singleColumn() : grid;
            children = Contract.immutableList(children);
        }

        @Override
        public NodeKind kind() {
            return NodeKind.REGION;
        }
    }

    /**
     * A labelled, optionally collapsible sub-grid.
     *
     * @param label an i18n key, never a literal caption — captions are resolved by the client
     * @param span  how many of the parent's columns this group occupies, per breakpoint
     * @param grid  the group's own internal column counts
     */
    record Group(String id,
                 String label,
                 boolean collapsible,
                 Map<String, Integer> span,
                 GridSpec grid,
                 List<LayoutNode> children) implements LayoutNode {

        public Group {
            id = Contract.requireId(id, "group id");
            span = Span.normalise(span);
            grid = grid == null ? GridSpec.singleColumn() : grid;
            children = Contract.immutableList(children);
        }

        @Override
        public NodeKind kind() {
            return NodeKind.GROUP;
        }
    }

    /**
     * A leaf holding exactly one component. Slots never overlap: the grid has no z-axis, which is
     * what removes the whole class of layout defects that absolute positioning produces.
     */
    record Slot(String id, Map<String, Integer> span, ComponentRef component) implements LayoutNode {

        public Slot {
            id = Contract.requireId(id, "slot id");
            span = Span.normalise(span);
            if (component == null) {
                throw new IllegalArgumentException("slot '" + id + "' has no component");
            }
        }

        @Override
        public NodeKind kind() {
            return NodeKind.SLOT;
        }
    }
}
