package com.zetcom.mpria.config.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code catalog-service} calls {@code FieldType.valueOf(type)} on the {@code type} string this
 * service publishes. The two enums are a wire contract held together by nothing but their names,
 * so this test pins them: adding, removing or renaming a constant here breaks a shipped service,
 * and it should break this test first.
 */
class FieldTypeTest {

    @Test
    void declares_exactly_the_seven_types_named_in_the_contract() {
        assertThat(FieldType.values()).extracting(Enum::name).containsExactly(
                "STRING", "TEXT", "INTEGER", "DECIMAL", "BOOLEAN", "DATE", "ENUM");
    }
}
