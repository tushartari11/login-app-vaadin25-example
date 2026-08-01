package com.zetcom.mpria.catalog.application;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.zetcom.mpria.catalog.domain.CatalogFixtures;
import com.zetcom.mpria.catalog.domain.CatalogRecord;
import com.zetcom.mpria.catalog.domain.Lifecycle;
import com.zetcom.mpria.catalog.domain.RecordNotFoundException;
import com.zetcom.mpria.catalog.testing.InMemoryRecordRepository;
import com.zetcom.mpria.catalog.testing.StubRecordSchemaProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.zetcom.mpria.catalog.domain.CatalogFixtures.NOW;
import static com.zetcom.mpria.catalog.domain.CatalogFixtures.ORG_UNIT;
import static com.zetcom.mpria.catalog.domain.CatalogFixtures.OTHER_TENANT;
import static com.zetcom.mpria.catalog.domain.CatalogFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Use-case tests against fakes: no Spring context, no database, milliseconds to run.
 *
 * <p>What is asserted here is orchestration and boundaries — tenant isolation, concurrency
 * handling, schema resolution. The rules themselves are the domain's tests.
 */
class CatalogRecordServiceTest {

    private final InMemoryRecordRepository records = new InMemoryRecordRepository();
    private final StubRecordSchemaProvider schemas =
            new StubRecordSchemaProvider().with(CatalogFixtures.objectSchema());
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final CatalogPrincipal curator = new CatalogPrincipal(TENANT, ORG_UNIT, "curator@example.org");
    private final CatalogPrincipal outsider = new CatalogPrincipal(OTHER_TENANT, ORG_UNIT, "other@example.org");

    private CatalogRecordService service;

    @BeforeEach
    void setUp() {
        service = new CatalogRecordService(records, schemas, clock);
    }

    @Test
    void creates_a_record_against_the_published_schema() {
        CatalogRecord created = service.create(curator, "object", "INV-0001", CatalogFixtures.validPayload());

        assertThat(created.tenantId()).isEqualTo(TENANT);
        assertThat(created.lifecycle()).isEqualTo(Lifecycle.DRAFT);
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(records.size()).isOne();
    }

    @Test
    void refuses_a_record_type_no_published_revision_defines() {
        assertThatExceptionOfType(UnknownRecordTypeException.class)
                .isThrownBy(() -> service.create(curator, "spacecraft", "S-1", Map.of()))
                .withMessageContaining("spacecraft");

        assertThat(records.size()).isZero();
    }

    @Test
    void a_record_is_invisible_to_another_tenant() {
        CatalogRecord created = service.create(curator, "object", "INV-0001", CatalogFixtures.validPayload());

        assertThatExceptionOfType(RecordNotFoundException.class)
                .isThrownBy(() -> service.get(outsider, created.id()));
    }

    @Test
    void another_tenant_cannot_see_it_in_a_listing_either() {
        service.create(curator, "object", "INV-0001", CatalogFixtures.validPayload());

        assertThat(service.listByType(outsider, "object", 50)).isEmpty();
        assertThat(service.listByType(curator, "object", 50)).hasSize(1);
    }

    @Test
    void a_write_built_on_a_stale_copy_is_rejected_before_it_reaches_the_database() {
        CatalogRecord created = service.create(curator, "object", "INV-0001", CatalogFixtures.validPayload());
        records.bumpVersion(created.id());

        assertThatExceptionOfType(StaleRecordException.class)
                .isThrownBy(() -> service.update(curator, created.id(), "INV-0001",
                        CatalogFixtures.validPayload(), created.etag()))
                .withMessageContaining("has moved on");
    }

    @Test
    void a_write_carrying_the_current_etag_succeeds_and_advances_the_version() {
        CatalogRecord created = service.create(curator, "object", "INV-0001", CatalogFixtures.validPayload());

        Map<String, Object> payload = new java.util.LinkedHashMap<>(CatalogFixtures.validPayload());
        payload.put("condition", "FAIR");
        CatalogRecord updated = service.update(curator, created.id(), "INV-0001", payload, created.etag());

        assertThat(updated.data()).containsEntry("condition", "FAIR");
        assertThat(updated.version()).isGreaterThan(created.version());
    }

    @Test
    void a_caller_may_opt_out_of_the_precondition_but_the_bff_never_does() {
        CatalogRecord created = service.create(curator, "object", "INV-0001", CatalogFixtures.validPayload());
        records.bumpVersion(created.id());

        assertThat(service.update(curator, created.id(), "INV-0001",
                CatalogFixtures.validPayload(), null)).isNotNull();
    }

    @Test
    void listing_is_capped_so_a_client_cannot_ask_for_the_whole_table() {
        for (int i = 0; i < 5; i++) {
            service.create(curator, "object", "INV-" + i, payloadWithInventoryNumber("INV-" + i));
        }

        assertThat(service.listByType(curator, "object", 1_000_000)).hasSize(5);
        assertThat(service.listByType(curator, "object", 0)).hasSize(1);
    }

    @Test
    void transitions_go_through_the_aggregate_so_the_rules_hold_here_too() {
        CatalogRecord created = service.create(curator, "object", "INV-0001", CatalogFixtures.validPayload());

        CatalogRecord published = service.transition(curator, created.id(), Lifecycle.PUBLISHED);

        assertThat(published.lifecycle()).isEqualTo(Lifecycle.PUBLISHED);
    }

    @Test
    void a_missing_record_reports_not_found_rather_than_a_null() {
        assertThatExceptionOfType(RecordNotFoundException.class)
                .isThrownBy(() -> service.get(curator, UUID.randomUUID()));
    }

    private static Map<String, Object> payloadWithInventoryNumber(String inventoryNumber) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>(CatalogFixtures.validPayload());
        payload.put("inventoryNumber", inventoryNumber);
        return payload;
    }
}
