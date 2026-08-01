package com.zetcom.mpria.config.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.zetcom.mpria.pm.ComponentRef;
import com.zetcom.mpria.pm.LayoutNode;

/**
 * The publish gate.
 *
 * <p>Validation is a server-side gate, not a convenience the console could skip: a published
 * revision is cached indefinitely by every consumer, so a configuration that would render a broken
 * screen has to be refused while the person who wrote it is still looking at it.
 *
 * <p>Every rejection names the screen, the node and the offending id, and validation collects all
 * of them rather than stopping at the first. An SME fixing a form should see the whole list once,
 * not discover it one publish attempt at a time.
 */
public final class ConfigurationValidator {

    private final ComponentRegistry registry;

    public ConfigurationValidator(ComponentRegistry registry) {
        this.registry = registry;
    }

    /** @return every reason {@code revision} cannot be published, in document order; empty when it can. */
    public List<ConfigurationError> validate(ConfigurationRevision revision) {
        List<ConfigurationError> errors = new ArrayList<>();
        for (ScreenLayout screen : revision.screens()) {
            Optional<RecordTypeDefinition> recordType = revision.recordType(screen.recordType());
            if (recordType.isEmpty()) {
                // One cause, one error: reporting each binding of a screen whose record type is
                // missing would bury the single change that fixes all of them.
                errors.add(new ConfigurationError(screen.screenId(), null,
                        "references record type '" + screen.recordType()
                                + "', which this revision does not define"));
                continue;
            }
            screen.layout().walk(node -> {
                if (node instanceof LayoutNode.Slot slot) {
                    inspect(screen, slot, recordType.get()).ifPresent(errors::add);
                }
            });
        }
        return List.copyOf(errors);
    }

    /** @throws ConfigurationValidationException carrying every problem, when there is one. */
    public void requireValid(ConfigurationRevision revision) {
        List<ConfigurationError> errors = validate(revision);
        if (!errors.isEmpty()) {
            throw new ConfigurationValidationException(errors);
        }
    }

    private Optional<ConfigurationError> inspect(ScreenLayout screen,
                                                 LayoutNode.Slot slot,
                                                 RecordTypeDefinition recordType) {
        ComponentRef component = slot.component();
        Optional<ComponentDefinition> definition = registry.find(component.type());
        if (definition.isEmpty()) {
            return error(screen, slot, "uses component type '" + component.type()
                    + "', which is not in the component registry");
        }
        ComponentDefinition drawn = definition.get();

        if (!component.isBound()) {
            return drawn.requiresFieldBinding()
                    ? error(screen, slot, "uses component '" + drawn.type()
                            + "', which requires a field binding")
                    : Optional.empty();
        }
        if (!drawn.requiresFieldBinding()) {
            return error(screen, slot, "binds field '" + component.fieldId() + "' to component '"
                    + drawn.type() + "', which accepts no field");
        }

        Optional<FieldDefinition> field = recordType.field(component.fieldId());
        if (field.isEmpty()) {
            return error(screen, slot, "binds field '" + component.fieldId()
                    + "', which record type '" + recordType.recordType() + "' does not define");
        }
        if (!drawn.accepts(field.get().type())) {
            return error(screen, slot, "binds field '" + component.fieldId() + "' of type "
                    + field.get().type() + " to component '" + drawn.type()
                    + "', which accepts " + drawn.acceptsFieldTypes());
        }
        return Optional.empty();
    }

    private static Optional<ConfigurationError> error(ScreenLayout screen,
                                                      LayoutNode.Slot slot,
                                                      String message) {
        return Optional.of(new ConfigurationError(screen.screenId(), slot.id(), message));
    }
}
