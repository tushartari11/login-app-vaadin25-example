package com.zetcom.mpria.pm;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The runtime state of one field, as computed by the presentation model.
 *
 * <p>The renderer applies {@code readOnly}, {@code required} and {@code hidden} mechanically and
 * has no idea <em>why</em> they hold. The reason may be the record's lifecycle, the principal's
 * permissions, a configured visibility expression, or all three — composing those is a domain
 * concern and stays on the server, which is what stops client and server authorisation drifting
 * apart.
 *
 * @param type      the field's data type, e.g. {@code STRING}, {@code INTEGER}, {@code DATE}
 * @param label     an i18n key resolved by the client
 * @param value     the current value, {@code null} when unset
 * @param options   allowed values for enumerated or vocabulary-backed fields
 * @param errors    validation messages for the current value; empty when valid
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldState(String type,
                         String label,
                         Object value,
                         List<FieldOption> options,
                         boolean readOnly,
                         boolean required,
                         boolean hidden,
                         List<String> errors) {

    public FieldState {
        type = Contract.requireId(type, "field type");
        options = Contract.immutableList(options);
        errors = Contract.immutableList(errors);
    }

    /** An editable field with no constraints applied. */
    public static FieldState editable(String type, String label, Object value) {
        return new FieldState(type, label, value, List.of(), false, false, false, List.of());
    }

    public FieldState asReadOnly() {
        return new FieldState(type, label, value, options, true, required, hidden, errors);
    }

    public FieldState asRequired() {
        return new FieldState(type, label, value, options, readOnly, true, hidden, errors);
    }

    public FieldState withErrors(List<String> messages) {
        return new FieldState(type, label, value, options, readOnly, required, hidden, messages);
    }

    public FieldState withOptions(List<FieldOption> allowed) {
        return new FieldState(type, label, value, allowed, readOnly, required, hidden, errors);
    }
}
