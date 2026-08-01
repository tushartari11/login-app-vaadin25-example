package com.zetcom.mpria.bff.testing;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * A validated access token in the security context, for adapter tests.
 *
 * <p>The outbound adapters take the tenant and the bearer token from the security context rather
 * than from their arguments, so a test of one has to establish a caller the way the filter chain
 * would.
 */
public final class CallerAuthentication {

    public static final String TOKEN = "caller-access-token";

    private CallerAuthentication() {
    }

    public static void install() {
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .subject("curator@example.org")
                .claim("tenant_id", ScreenFixtures.TENANT_ID.toString())
                .issuedAt(Instant.parse("2026-08-01T10:00:00Z"))
                .expiresAt(Instant.parse("2026-08-01T11:00:00Z"))
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("SCOPE_mpria"))));
    }

    /** Installs a caller whose token carries no usable tenant claim. */
    public static void installWithoutTenant() {
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .subject("curator@example.org")
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
