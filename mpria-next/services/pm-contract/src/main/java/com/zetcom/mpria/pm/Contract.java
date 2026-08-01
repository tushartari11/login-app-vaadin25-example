package com.zetcom.mpria.pm;

import java.util.List;

/** Shared argument checks for the contract records. Package-private by intent. */
final class Contract {

    private Contract() {
    }

    static String requireId(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(what + " must not be blank");
        }
        return value;
    }

    static <T> List<T> immutableList(List<T> value) {
        return value == null ? List.of() : List.copyOf(value);
    }
}
