package com.zetcom.mpria.catalog.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static com.zetcom.mpria.catalog.domain.CatalogFixtures.NOW;
import static com.zetcom.mpria.catalog.domain.CatalogFixtures.ORG_UNIT;
import static com.zetcom.mpria.catalog.domain.CatalogFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class CatalogRecordTest {

    private final RecordSchema schema = CatalogFixtures.objectSchema();

    @Test
    void a_new_record_starts_as_a_draft_stamped_with_the_schema_version_it_was_written_against() {
        CatalogRecord record = CatalogFixtures.newRecord();

        assertThat(record.lifecycle()).isEqualTo(Lifecycle.DRAFT);
        assertThat(record.schemaVersion()).isEqualTo(1);
        assertThat(record.title()).isEqualTo("Study of a hand");
        assertThat(record.version()).isZero();
        assertThat(record.createdAt()).isEqualTo(NOW);
    }

    @Test
    void an_invalid_payload_never_becomes_a_record() {
        assertThatExceptionOfType(RecordValidationException.class)
                .isThrownBy(() -> CatalogRecord.create(UUID.randomUUID(), TENANT, ORG_UNIT, schema,
                        "INV-1", Map.of("title", "no inventory number"), "curator", NOW))
                .satisfies(e -> assertThat(e.errors()).containsKey("inventoryNumber"));
    }

    @Test
    void falls_back_to_the_identifier_when_the_title_field_has_no_value() {
        RecordSchema untitled = new RecordSchema("object", 1,
                List.of(FieldDefinition.text("title")), "title");

        CatalogRecord record = CatalogRecord.create(UUID.randomUUID(), TENANT, ORG_UNIT, untitled,
                "INV-0002", Map.of(), "curator", NOW);

        assertThat(record.title()).isEqualTo("INV-0002");
    }

    @Test
    void updating_re_stamps_the_schema_version_so_records_migrate_forward_as_they_are_edited() {
        CatalogRecord record = CatalogFixtures.newRecord();
        RecordSchema v2 = new RecordSchema("object", 2, schema.fields(), "title");

        Map<String, Object> payload = new LinkedHashMap<>(CatalogFixtures.validPayload());
        payload.put("title", "Study of a hand, verso");
        record.update(v2, "INV-0001", payload, "registrar@example.org", NOW.plusSeconds(60));

        assertThat(record.schemaVersion()).isEqualTo(2);
        assertThat(record.title()).isEqualTo("Study of a hand, verso");
        assertThat(record.updatedBy()).isEqualTo("registrar@example.org");
        assertThat(record.createdBy()).isEqualTo("curator@example.org");
    }

    @Test
    void refuses_a_schema_belonging_to_a_different_record_type() {
        CatalogRecord record = CatalogFixtures.newRecord();
        RecordSchema personSchema = new RecordSchema("person", 1,
                List.of(FieldDefinition.text("name")), "name");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> record.update(personSchema, "P-1", Map.of("name", "x"), "a", NOW))
                .withMessageContaining("cannot apply a 'person' schema to a 'object' record");
    }

    @Test
    void an_archived_record_cannot_be_edited() {
        CatalogRecord record = CatalogFixtures.newRecord();
        record.transitionTo(Lifecycle.ARCHIVED, "curator", NOW);

        assertThatIllegalStateException()
                .isThrownBy(() -> record.update(schema, "INV-0001", CatalogFixtures.validPayload(), "a", NOW))
                .withMessageContaining("ARCHIVED");
    }

    @Test
    void archiving_is_terminal() {
        CatalogRecord record = CatalogFixtures.newRecord();
        record.transitionTo(Lifecycle.ARCHIVED, "curator", NOW);

        assertThatIllegalStateException()
                .isThrownBy(() -> record.transitionTo(Lifecycle.DRAFT, "curator", NOW))
                .withMessageContaining("from ARCHIVED to DRAFT");
    }

    @Test
    void a_published_record_may_be_returned_to_draft_and_is_still_editable() {
        CatalogRecord record = CatalogFixtures.newRecord();

        record.transitionTo(Lifecycle.PUBLISHED, "curator", NOW);
        assertThat(record.lifecycle().allowsEditing()).isTrue();

        record.transitionTo(Lifecycle.DRAFT, "curator", NOW.plusSeconds(1));
        assertThat(record.lifecycle()).isEqualTo(Lifecycle.DRAFT);
    }

    @Test
    void the_payload_it_hands_out_cannot_be_used_to_mutate_it() {
        CatalogRecord record = CatalogFixtures.newRecord();
        Map<String, Object> exposed = record.data();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> exposed.put("condition", "POOR"));
        assertThat(record.data()).containsEntry("condition", "GOOD");
    }

    @Test
    void the_etag_tracks_the_persisted_version() {
        CatalogRecord stored = CatalogRecord.rehydrate(UUID.randomUUID(), TENANT, "object", ORG_UNIT,
                1, "INV-1", "t", Lifecycle.DRAFT, Map.of(), NOW, "a", NOW, "a", 7L);

        assertThat(stored.etag()).isEqualTo("\"7\"");
    }

    @Test
    void update_leaves_creation_audit_untouched() {
        CatalogRecord record = CatalogFixtures.newRecord();
        Instant later = NOW.plusSeconds(3600);

        record.update(schema, "INV-0001", CatalogFixtures.validPayload(), "someone-else", later);

        assertThat(record.createdAt()).isEqualTo(NOW);
        assertThat(record.updatedAt()).isEqualTo(later);
    }
}
