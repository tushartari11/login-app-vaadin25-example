package com.zetcom.mpria.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data query interface.
 *
 * <p>Every finder is keyed on {@code tenantId} as well as the natural key. Nothing in this
 * interface can read across tenants, which is the property that has to hold before the SaaS phase
 * moves several customers into one database.
 */
interface CatalogRecordJpaRepository extends JpaRepository<CatalogRecordEntity, UUID> {

    Optional<CatalogRecordEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CatalogRecordEntity> findByTenantIdAndRecordTypeOrderByUpdatedAtDesc(
            UUID tenantId, String recordType, Limit limit);
}
