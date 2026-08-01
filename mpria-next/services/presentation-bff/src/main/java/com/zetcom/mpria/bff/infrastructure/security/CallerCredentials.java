package com.zetcom.mpria.bff.infrastructure.security;

import java.util.Optional;
import java.util.UUID;

import com.zetcom.mpria.bff.application.IncompleteCallerTokenException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The acting caller, taken from the validated access token.
 *
 * <p>The tenant comes from the token's {@code tenant_id} claim and never from a request parameter.
 * A client cannot nominate the tenant it is reading; only the identity provider can.
 *
 * <p><strong>Skeleton scope.</strong> Outbound calls relay the caller's own token. The target design
 * is the client-credentials grant with a per-service client and an audience restriction, so a
 * compromised BFF token cannot be replayed against config-service (ARCHITECTURE.md §10). That is
 * Phase-1 work; relaying is enough to make the walking skeleton walk, and it is deliberately
 * confined to this one class so that replacing it touches nothing else.
 */
public final class CallerCredentials {

    private static final String TENANT_CLAIM = "tenant_id";

    private CallerCredentials() {
    }

    /** Adds the caller's bearer token, if there is one. Absent outside a request, e.g. on startup. */
    public static void relayBearerToken(HttpHeaders headers) {
        callerToken().ifPresent(jwt -> headers.setBearerAuth(jwt.getTokenValue()));
    }

    /** The tenant this request acts for. */
    public static UUID tenantId() {
        String value = callerToken()
                .map(jwt -> jwt.getClaimAsString(TENANT_CLAIM))
                .orElseThrow(() -> new IncompleteCallerTokenException(TENANT_CLAIM));
        if (value == null || value.isBlank()) {
            throw new IncompleteCallerTokenException(TENANT_CLAIM);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IncompleteCallerTokenException(TENANT_CLAIM);
        }
    }

    private static Optional<Jwt> callerToken() {
        if (SecurityContextHolder.getContext().getAuthentication()
                instanceof JwtAuthenticationToken authentication) {
            return Optional.of(authentication.getToken());
        }
        return Optional.empty();
    }
}
