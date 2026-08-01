package com.zetcom.mpria.pm;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SpanTest {

    @Nested
    @DisplayName("normalise")
    class Normalise {

        @Test
        void defaults_to_single_column_so_an_unconfigured_layout_is_mobile_correct() {
            assertThat(Span.normalise(null)).containsExactly(Map.entry("base", 1));
            assertThat(Span.normalise(Map.of())).containsExactly(Map.entry("base", 1));
        }

        @Test
        void orders_breakpoints_smallest_to_largest_so_serialised_layouts_are_diffable() {
            Map<String, Integer> unordered = new LinkedHashMap<>();
            unordered.put("xl", 12);
            unordered.put("base", 1);
            unordered.put("md", 6);

            assertThat(Span.normalise(unordered))
                    .containsExactly(Map.entry("base", 1), Map.entry("md", 6), Map.entry("xl", 12));
        }

        @Test
        void rejects_unknown_breakpoint_names() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Span.normalise(Map.of("tablet", 4)))
                    .withMessageContaining("tablet")
                    .withMessageContaining("[base, sm, md, lg, xl]");
        }

        @Test
        void rejects_a_span_smaller_than_one_column() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Span.normalise(Map.of("md", 0)))
                    .withMessageContaining("md")
                    .withMessageContaining(">= 1");
        }

        @Test
        void returns_an_immutable_copy() {
            Map<String, Integer> mutable = new LinkedHashMap<>(Map.of("base", 1));
            Map<String, Integer> normalised = Span.normalise(mutable);
            mutable.put("md", 6);

            assertThat(normalised).doesNotContainKey("md");
        }
    }

    @Nested
    @DisplayName("resolve cascades upward from the nearest smaller breakpoint")
    class Resolve {

        private final Map<String, Integer> span = Span.normalise(Map.of("base", 1, "md", 6));

        @ParameterizedTest(name = "{0} resolves to {1}")
        @CsvSource({ "base,1", "sm,1", "md,6", "lg,6", "xl,6" })
        void cascade(String breakpoint, int expected) {
            assertThat(Span.resolve(span, breakpoint)).isEqualTo(expected);
        }

        @Test
        void falls_back_to_one_column_when_nothing_is_defined_below() {
            assertThat(Span.resolve(Map.of("xl", 12), "base")).isEqualTo(1);
        }

        @Test
        void rejects_an_unknown_breakpoint() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Span.resolve(span, "phone"));
        }
    }
}
