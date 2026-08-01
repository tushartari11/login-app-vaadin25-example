package com.zetcom.mpria.catalog.domain;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * The data types a configured field may have.
 *
 * <p>This enum is the whole type system of the configurable data model. It is deliberately short:
 * every entry costs a validator here, a component in every renderer, and an index strategy in the
 * search projection. Anything expressible as a constrained {@code STRING} should be one.
 */
public enum FieldType {

    STRING {
        @Override
        Optional<String> reject(Object value) {
            return value instanceof String ? Optional.empty() : typeMismatch("a string");
        }
    },

    TEXT {
        @Override
        Optional<String> reject(Object value) {
            return value instanceof String ? Optional.empty() : typeMismatch("a string");
        }
    },

    INTEGER {
        @Override
        Optional<String> reject(Object value) {
            if (value instanceof Integer || value instanceof Long) {
                return Optional.empty();
            }
            // JSON numbers arrive as Number; reject anything with a fractional part.
            if (value instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue())) {
                return Optional.empty();
            }
            return typeMismatch("a whole number");
        }
    },

    DECIMAL {
        @Override
        Optional<String> reject(Object value) {
            return value instanceof Number ? Optional.empty() : typeMismatch("a number");
        }
    },

    BOOLEAN {
        @Override
        Optional<String> reject(Object value) {
            return value instanceof Boolean ? Optional.empty() : typeMismatch("true or false");
        }
    },

    DATE {
        @Override
        Optional<String> reject(Object value) {
            if (!(value instanceof String text)) {
                return typeMismatch("an ISO-8601 date");
            }
            try {
                LocalDate.parse(text);
                return Optional.empty();
            } catch (DateTimeParseException e) {
                return Optional.of("must be an ISO-8601 date (yyyy-MM-dd)");
            }
        }
    },

    /** Constrained to the field definition's allowed values. */
    ENUM {
        @Override
        Optional<String> reject(Object value) {
            return value instanceof String ? Optional.empty() : typeMismatch("one of the allowed values");
        }
    };

    /** @return a validation message when {@code value} does not fit this type, else empty. */
    abstract Optional<String> reject(Object value);

    private static Optional<String> typeMismatch(String expectation) {
        return Optional.of("must be " + expectation);
    }
}
