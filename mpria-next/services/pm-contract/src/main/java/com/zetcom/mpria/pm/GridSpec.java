package com.zetcom.mpria.pm;

import java.util.Map;

/**
 * The column count of a grid container, per breakpoint, plus a gap token.
 *
 * <p>Maps directly onto CSS {@code grid-template-columns}. The gap is a design-system token name
 * ({@code xs}…{@code xl}), not a length, so spacing stays consistent across every configured
 * screen regardless of who configured it.
 */
public record GridSpec(Map<String, Integer> columns, String gap) {

    private static final String DEFAULT_GAP = "md";

    public GridSpec {
        columns = Span.normalise(columns);
        gap = (gap == null || gap.isBlank()) ? DEFAULT_GAP : gap;
    }

    public static GridSpec singleColumn() {
        return new GridSpec(Map.of("base", 1), DEFAULT_GAP);
    }

    public static GridSpec of(Map<String, Integer> columns) {
        return new GridSpec(columns, DEFAULT_GAP);
    }

    /** Effective column count at {@code breakpoint}, cascading from smaller breakpoints. */
    public int columnsAt(String breakpoint) {
        return Span.resolve(columns, breakpoint);
    }
}
