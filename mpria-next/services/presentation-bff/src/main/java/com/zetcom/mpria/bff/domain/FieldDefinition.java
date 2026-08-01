package com.zetcom.mpria.bff.domain;

import java.util.List;

/**
 * One field as the published configuration defines it.
 *
 * <p>{@code type} is deliberately a {@code String} and not an enum. This service never validates a
 * value — catalog-service does that against the same schema — it only relays the type so the
 * renderer can choose a component. Modelling the type system here would mean a new field type could
 * not be configured without also redeploying the BFF, which is exactly the coupling the
 * configuration-over-code rule exists to prevent.
 *
 * @param label         an i18n key resolved by the client, never a literal caption
 * @param allowedValues the enumerated values, in the order configuration declared them. A list
 *                      rather than a set: option order is visible to the user, so it is data
 */
public record FieldDefinition(String id,
                              String type,
                              String label,
                              boolean required,
                              List<String> allowedValues) {

    public FieldDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("field id must not be blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("field '" + id + "' has no type");
        }
        allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
    }
}
