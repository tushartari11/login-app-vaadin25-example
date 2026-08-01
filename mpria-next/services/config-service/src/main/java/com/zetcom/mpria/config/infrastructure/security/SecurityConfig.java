package com.zetcom.mpria.config.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * The service is an OAuth 2 resource server: it validates a JWT against the realm's JWKS and holds
 * no session of its own.
 *
 * <p>Three tiers of access, and the difference between them is the point:
 * <ul>
 *   <li>reading configuration needs only an authenticated principal — every operator client
 *       resolves layouts, so it is not an administrative privilege;</li>
 *   <li>editing a draft needs {@code mpria-config-admin}, because a draft is what the designer
 *       writes to;</li>
 *   <li>publishing needs {@code mpria-config-publisher} <em>as well</em>. Editing a draft affects
 *       nobody; publishing changes what every operator in the tenant sees on their next screen.</li>
 * </ul>
 *
 * <p>Public because the slice tests import it: the 403s above are part of the API contract, so
 * they are asserted against the real filter chain rather than assumed.
 */
@Configuration
public class SecurityConfig {

    private static final String CONFIG_ADMIN = "mpria-config-admin";
    private static final String CONFIG_PUBLISHER = "mpria-config-publisher";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/drafts/*/publish")
                        .access(publisherWhoIsAlsoAnAdmin())
                        .requestMatchers("/api/v1/admin/**").hasRole(CONFIG_ADMIN)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(realmRoleAuthentication())))
                // No browser reaches this service directly; the admin console sends a bearer token.
                // CSRF protection defends cookie-authenticated requests, and there are none here.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    private static AuthorizationManager<RequestAuthorizationContext> publisherWhoIsAlsoAnAdmin() {
        return AuthorizationManagers.allOf(
                AuthorityAuthorizationManager.hasRole(CONFIG_ADMIN),
                AuthorityAuthorizationManager.hasRole(CONFIG_PUBLISHER));
    }

    private static JwtAuthenticationConverter realmRoleAuthentication() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RealmRoleAuthoritiesConverter());
        return converter;
    }
}
