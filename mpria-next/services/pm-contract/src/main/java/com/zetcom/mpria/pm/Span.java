package com.zetcom.mpria.pm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Column spans keyed by named breakpoint.
 *
 * <p>Breakpoints are names, never pixel values: an administrator arranging a form never types a
 * width. Pixel values for each name live once, in the client's design tokens.
 *
 * <p>Spans cascade upward. A layout that only defines {@code base} works at every size, so the
 * default state of any configuration is a correct single-column mobile layout. Larger breakpoints
 * are overridden only where a different arrangement is actually wanted.
 */
public final class Span {

    /** Ordered smallest to largest. The order is what makes {@link #resolve} a cascade. */
    public static final List<String> BREAKPOINTS = List.of("base", "sm", "md", "lg", "xl");

    private static final Map<String, Integer> DEFAULT = Map.of("base", 1);

    private Span() {
    }

    /**
     * Validates breakpoint names and span values, and returns an immutable copy ordered smallest
     * to largest so that serialised layouts are stable and diff-able.
     */
    public static Map<String, Integer> normalise(Map<String, Integer> span) {
        if (span == null || span.isEmpty()) {
            return DEFAULT;
        }
        Map<String, Integer> ordered = new LinkedHashMap<>();
        for (String breakpoint : BREAKPOINTS) {
            Integer value = span.get(breakpoint);
            if (value != null) {
                if (value < 1) {
                    throw new IllegalArgumentException(
                            "span for breakpoint '" + breakpoint + "' must be >= 1 but was " + value);
                }
                ordered.put(breakpoint, value);
            }
        }
        if (ordered.size() != span.size()) {
            Map<String, Integer> unknown = new LinkedHashMap<>(span);
            BREAKPOINTS.forEach(unknown::remove);
            throw new IllegalArgumentException("unknown breakpoint(s): " + unknown.keySet()
                    + "; expected any of " + BREAKPOINTS);
        }
        // Not Map.copyOf: that returns an unordered immutable map, which would make serialised
        // layouts churn between builds and turn every configuration export into a noisy diff.
        return Collections.unmodifiableMap(ordered);
    }

    /**
     * Resolves the effective value at {@code breakpoint} by cascading from the largest defined
     * breakpoint at or below it, falling back to {@code base} and finally to {@code 1}.
     */
    public static int resolve(Map<String, Integer> span, String breakpoint) {
        int index = BREAKPOINTS.indexOf(breakpoint);
        if (index < 0) {
            throw new IllegalArgumentException("unknown breakpoint '" + breakpoint + "'");
        }
        for (int i = index; i >= 0; i--) {
            Integer value = span.get(BREAKPOINTS.get(i));
            if (value != null) {
                return value;
            }
        }
        return 1;
    }
}
