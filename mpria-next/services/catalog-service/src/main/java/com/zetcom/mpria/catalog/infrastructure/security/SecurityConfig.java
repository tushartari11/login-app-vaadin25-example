package com.zetcom.mpria.catalog.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The service is an OAuth 2 resource server: it validates a JWT against the realm's JWKS and holds
 * no session of its own.
 *
 * <p>That statelessness is what makes horizontal scaling free — there is no session affinity to
 * preserve, which is the constraint the Vaadin 8 application could never escape.
 *
 * <p>Only coarse authorisation lives here (can this principal reach this endpoint). Whether a
 * principal may edit <em>this particular record's</em> organisational unit is a domain decision and
 * is applied in the application layer.
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // No browser reaches this service directly — the BFF does, bearing a token.
                // CSRF protection defends cookie-authenticated requests, and there are none here.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}
