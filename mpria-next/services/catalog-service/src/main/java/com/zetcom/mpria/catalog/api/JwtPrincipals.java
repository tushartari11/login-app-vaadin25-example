package com.zetcom.mpria.catalog.api;

import java.util.UUID;

import com.zetcom.mpria.catalog.application.CatalogPrincipal;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Derives the acting principal from a validated access token.
 *
 * <p>Tenant and organisational unit come from the token, never from a request parameter or header.
 * A client cannot nominate the tenant it is writing to; only the identity provider can.
 */
final class JwtPrincipals {

    static final String TENANT_CLAIM = "tenant_id";
    static final String ORG_UNIT_CLAIM = "org_unit_id";

    private JwtPrincipals() {
    }

    static CatalogPrincipal from(Jwt jwt) {
        return new CatalogPrincipal(
                requiredUuid(jwt, TENANT_CLAIM),
                requiredUuid(jwt, ORG_UNIT_CLAIM),
                jwt.getSubject());
    }

    private static UUID requiredUuid(Jwt jwt, String claim) {
        String value = jwt.getClaimAsString(claim);
        if (value == null || value.isBlank()) {
            throw new MissingClaimException(claim);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new MissingClaimException(claim);
        }
    }

    /** The token is valid but does not carry the claims this service needs to act. */
    static class MissingClaimException extends RuntimeException {
        MissingClaimException(String claim) {
            super("access token is missing a usable '" + claim + "' claim");
        }
    }
}
