package com.zetcom.mpria.config.infrastructure.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keycloak puts realm roles under {@code realm_access.roles}; Spring's default converter reads
 * {@code scope}. Get this wrong and every administration endpoint answers 403 for a token that
 * plainly carries the role — a failure that looks like a policy decision rather than a bug.
 */
class RealmRoleAuthoritiesConverterTest {

    private final RealmRoleAuthoritiesConverter converter = new RealmRoleAuthoritiesConverter();

    @Test
    void maps_each_realm_role_to_the_prefixed_authority_hasRole_expects() {
        Jwt token = tokenWith(claims -> claims.put("realm_access",
                Map.of("roles", List.of("mpria-user", "mpria-config-admin"))));

        assertThat(converter.convert(token))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_mpria-user", "ROLE_mpria-config-admin");
    }

    @Test
    void grants_nothing_to_a_token_carrying_no_realm_roles() {
        assertThat(converter.convert(tokenWith(claims -> claims.put("scope", "openid")))).isEmpty();
    }

    @Test
    void grants_nothing_when_realm_access_is_present_but_shaped_unexpectedly() {
        // A token is attacker-influenced input. A malformed claim must grant nothing, not throw:
        // an exception here becomes a 500 on a request that should simply be unauthorised.
        Jwt token = tokenWith(claims -> claims.put("realm_access", Map.of("roles", "mpria-user")));

        assertThat(converter.convert(token)).isEmpty();
    }

    private static Jwt tokenWith(Consumer<Map<String, Object>> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("configadmin")
                .issuedAt(Instant.EPOCH)
                .expiresAt(Instant.EPOCH.plusSeconds(300));
        builder.claims(claims);
        return builder.build();
    }
}
