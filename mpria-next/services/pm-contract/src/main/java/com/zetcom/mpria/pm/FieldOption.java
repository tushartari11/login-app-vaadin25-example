package com.zetcom.mpria.pm;

/** One allowed value of an enumerated or vocabulary-backed field. */
public record FieldOption(String value, String label) {

    public FieldOption {
        value = Contract.requireId(value, "option value");
        label = (label == null || label.isBlank()) ? value : label;
    }
}
