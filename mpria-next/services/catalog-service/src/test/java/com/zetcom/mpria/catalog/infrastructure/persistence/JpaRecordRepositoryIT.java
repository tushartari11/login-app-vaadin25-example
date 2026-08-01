package com.zetcom.mpria.catalog.infrastructure.persistence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import com.zetcom.mpria.catalog.application.port.RecordRepository;
import com.zetcom.mpria.catalog.domain.CatalogFixtures;
import com.zetcom.mpria.catalog.domain.CatalogRecord;
import com.zetcom.mpria.catalog.domain.Lifecycle;
import com.zetcom.mpria.catalog.domain.RecordSchema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static com.zetcom.mpria.catalog.domain.CatalogFixtures.NOW;
import static com.zetcom.mpria.catalog.domain.CatalogFixtures.ORG_UNIT;
import static com.zetcom.mpria.catalog.domain.CatalogFixtures.OTHER_TENANT;
import static com.zetcom.mpria.catalog.domain.CatalogFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Real Postgres, real Flyway, real {@code jsonb}.
 *
 * <p>An in-memory database would not exercise the one thing that most needs exercising here: that
 * a configured payload survives a round trip through a {@code jsonb} column with its types intact.
 * H2's JSON emulation would happily pass a test that production fails.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaRecordRepository.class)
@Testcontainers
class JpaRecordRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    /** The resource-server decoder is irrelevant to persistence but is a context dependency. */
    @MockitoBean
    org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired
    RecordRepository repository;

    @Autowired
    EntityManager entityManager;

    private final RecordSchema schema = CatalogFixtures.objectSchema();

    @Test
    void a_configured_payload_survives_the_jsonb_round_trip_with_its_types_intact() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("inventoryNumber", "INV-0001");
        payload.put("title", "Study of a hand");
        payload.put("acquiredOn", "1998-04-12");

        CatalogRecord saved = repository.save(newRecord("INV-0001", payload));
        detach();

        CatalogRecord loaded = repository.findById(TENANT, saved.id()).orElseThrow();

        assertThat(loaded.data())
                .containsEntry("inventoryNumber", "INV-0001")
                .containsEntry("title", "Study of a hand")
                .containsEntry("acquiredOn", "1998-04-12");
        assertThat(loaded.lifecycle()).isEqualTo(Lifecycle.DRAFT);
        assertThat(loaded.schemaVersion()).isEqualTo(1);
    }

    @Test
    void the_payload_lands_in_a_queryable_jsonb_column_not_an_opaque_string() {
        repository.save(newRecord("INV-0002", CatalogFixtures.validPayload()));
        detach();

        // If the column were text, this jsonb operator would fail rather than return a row.
        Object title = entityManager
                .createNativeQuery("SELECT data ->> 'title' FROM catalog_record WHERE identifier = :id")
                .setParameter("id", "INV-0002")
                .getSingleResult();

        assertThat(title).isEqualTo("Study of a hand");
    }

    @Test
    void a_record_cannot_be_read_from_another_tenant() {
        CatalogRecord saved = repository.save(newRecord("INV-0003", CatalogFixtures.validPayload()));
        detach();

        assertThat(repository.findById(TENANT, saved.id())).isPresent();
        assertThat(repository.findById(OTHER_TENANT, saved.id())).isEmpty();
    }

    @Test
    void saving_an_existing_record_advances_the_version_rather_than_inserting_a_duplicate() {
        CatalogRecord saved = repository.save(newRecord("INV-0004", CatalogFixtures.validPayload()));
        detach();

        CatalogRecord loaded = repository.findById(TENANT, saved.id()).orElseThrow();
        loaded.update(schema, "INV-0004", CatalogFixtures.validPayload(), "registrar", NOW.plusSeconds(60));
        CatalogRecord updated = repository.save(loaded);
        detach();

        assertThat(updated.version()).isEqualTo(saved.version() + 1);
        assertThat(repository.findByType(TENANT, "object", 50)).hasSize(1);
    }

    @Test
    void a_write_from_a_stale_load_loses_the_race() {
        CatalogRecord saved = repository.save(newRecord("INV-0005", CatalogFixtures.validPayload()));
        detach();

        CatalogRecord stale = repository.findById(TENANT, saved.id()).orElseThrow();
        CatalogRecord winner = repository.findById(TENANT, saved.id()).orElseThrow();

        winner.update(schema, "INV-0005", CatalogFixtures.validPayload(), "first", NOW.plusSeconds(10));
        repository.save(winner);
        detach();

        stale.update(schema, "INV-0005", CatalogFixtures.validPayload(), "second", NOW.plusSeconds(20));
        assertThatExceptionOfType(
                com.zetcom.mpria.catalog.application.RecordConcurrentlyModifiedException.class)
                .isThrownBy(() -> repository.save(stale));
    }

    @Test
    void listings_are_scoped_by_tenant_and_type_and_return_the_most_recently_touched_first() {
        repository.save(newRecord("INV-0006", CatalogFixtures.validPayload(), NOW));
        repository.save(newRecord("INV-0007", CatalogFixtures.validPayload(), NOW.plusSeconds(60)));
        detach();

        List<CatalogRecord> found = repository.findByType(TENANT, "object", 50);

        assertThat(found).extracting(CatalogRecord::identifier).containsExactly("INV-0007", "INV-0006");
        assertThat(repository.findByType(OTHER_TENANT, "object", 50)).isEmpty();
        assertThat(repository.findByType(TENANT, "person", 50)).isEmpty();
    }

    @Test
    void the_limit_is_honoured_at_the_database_rather_than_in_memory() {
        for (int i = 10; i < 15; i++) {
            repository.save(newRecord("INV-00" + i, CatalogFixtures.validPayload(), NOW.plusSeconds(i)));
        }
        detach();

        assertThat(repository.findByType(TENANT, "object", 2)).hasSize(2);
    }

    private CatalogRecord newRecord(String identifier, Map<String, Object> payload) {
        return newRecord(identifier, payload, NOW);
    }

    private CatalogRecord newRecord(String identifier, Map<String, Object> payload, java.time.Instant at) {
        Map<String, Object> withIdentifier = new LinkedHashMap<>(payload);
        withIdentifier.put("inventoryNumber", identifier);
        return CatalogRecord.create(UUID.randomUUID(), TENANT, ORG_UNIT, schema,
                identifier, withIdentifier, "curator@example.org", at);
    }

    /** Forces the next read to hit the database instead of the persistence context. */
    private void detach() {
        entityManager.flush();
        entityManager.clear();
    }
}
