package com.zetcom.mpria.config.domain;

import java.util.List;

import com.zetcom.mpria.config.testing.ConfigFixtures;
import com.zetcom.mpria.pm.GridSpec;
import com.zetcom.mpria.pm.Layout;
import com.zetcom.mpria.pm.LayoutNode;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * A screen identifies itself twice — once as configuration metadata and once inside the layout the
 * renderer receives. The two must never disagree, or a consumer's answer depends on which one it
 * happened to read.
 */
class ScreenLayoutTest {

    @Test
    void exposes_the_field_ids_its_layout_binds() {
        assertThat(ConfigFixtures.objectDetailScreen().boundFieldIds())
                .containsExactly("inventoryNumber", "title", "description", "acquiredOn", "condition");
    }

    @Test
    void rejects_a_layout_that_names_a_different_screen() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ScreenLayout("object-list", "object", layout("object-detail", "object")))
                .withMessageContaining("layout declares screenId 'object-detail'");
    }

    @Test
    void rejects_a_layout_that_names_a_different_record_type() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ScreenLayout("object-detail", "acquisition", layout("object-detail", "object")))
                .withMessageContaining("layout declares recordType 'object'");
    }

    private static Layout layout(String screenId, String recordType) {
        return new Layout(screenId, recordType,
                List.of(new LayoutNode.Region("main", GridSpec.singleColumn(), List.of())));
    }
}
