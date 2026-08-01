package com.zetcom.mpria.pm;

import java.util.List;
import java.util.Map;

/**
 * A complete screen, ready to render.
 *
 * <p>This is the contract that makes the frontend replaceable. A client implements one function —
 * {@code render(PresentationModel) -> UI} — mapping {@code component.type} to a widget. It needs
 * no knowledge of record types, fields or business rules, so swapping React for anything else
 * means rewriting a renderer rather than an application.
 *
 * <p>Invariant enforced here: every field bound by the layout must be present in {@code fields}.
 * A renderer is therefore never asked to draw a slot it has no data for, which removes the most
 * common failure mode of server-driven UI.
 *
 * @param fields keyed by field id; see {@link FieldState}
 */
public record PresentationModel(ScreenMeta meta,
                                Layout layout,
                                Map<String, FieldState> fields,
                                List<ActionModel> actions) {

    public PresentationModel {
        if (meta == null) {
            throw new IllegalArgumentException("meta is required");
        }
        if (layout == null) {
            throw new IllegalArgumentException("layout is required");
        }
        fields = fields == null ? Map.of() : Map.copyOf(fields);
        actions = Contract.immutableList(actions);
        requireEveryBoundFieldPresent(layout, fields);
    }

    private static void requireEveryBoundFieldPresent(Layout layout, Map<String, FieldState> fields) {
        List<String> missing = layout.boundFieldIds().stream()
                .filter(fieldId -> !fields.containsKey(fieldId))
                .distinct()
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "layout '" + layout.screenId() + "' binds field(s) with no state: " + missing);
        }
    }
}
