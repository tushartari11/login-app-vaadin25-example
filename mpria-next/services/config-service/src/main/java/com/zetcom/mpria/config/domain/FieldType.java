package com.zetcom.mpria.config.domain;

/**
 * The data types a configured field may have.
 *
 * <p>These seven names are a wire contract, not an internal detail: {@code catalog-service} reads
 * the {@code type} string this service publishes and calls {@code FieldType.valueOf} on it. A
 * constant renamed here is a runtime failure there.
 *
 * <p>The list is deliberately short. Every entry costs a validator in the catalogue, a component in
 * every renderer and an index strategy in the search projection, so anything expressible as a
 * constrained {@code STRING} should be one.
 */
public enum FieldType {

    STRING,
    TEXT,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    DATE,

    /** Constrained to the field definition's allowed values. */
    ENUM
}
