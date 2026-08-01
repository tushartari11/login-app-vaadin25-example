package com.zetcom.mpria.config.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Turns Keycloak realm roles into Spring Security authorities.
 *
 * <p>Keycloak puts realm roles under {@code realm_access.roles}, which is not where the default
 * converter looks — it reads {@code scope}. Without this, a token carrying
 * {@code mpria-config-admin} arrives with no authorities at all and every administration endpoint
 * answers 403.
 */
class RealmRoleAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS);
        if (realmAccess == null || !(realmAccess.get(ROLES) instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                // The ROLE_ prefix is what hasRole() expects; hasAuthority() would work without it,
                // but mixing the two conventions in one filter chain is how role checks go quiet.
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
