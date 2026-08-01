package com.zetcom.mpria.catalog.application;

import java.util.UUID;

/**
 * Who is acting, and in which tenant and organisational unit.
 *
 * <p>Derived from the validated JWT at the API boundary and passed explicitly through the
 * application layer. Explicit beats a thread-local: it is visible in every signature and trivial
 * to construct in a test.
 */
public record CatalogPrincipal(UUID tenantId, UUID orgUnitId, String userId) {

    public CatalogPrincipal {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (orgUnitId == null) {
            throw new IllegalArgumentException("orgUnitId is required");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}
