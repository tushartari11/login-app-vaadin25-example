package com.zetcom.mpria.bff.infrastructure.security;

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
 * <p>That statelessness matters more here than anywhere else in the platform. The BFF is on the
 * critical path of every screen (ADR-0003), so it has to scale horizontally, and it is precisely the
 * server-side session that made that impossible in the Vaadin 8 application.
 *
 * <p>Only coarse authorisation lives here (can this principal reach this endpoint). Whether a
 * principal may edit <em>this particular record</em> is a fine-grained decision belonging to the
 * domain services, never to the BFF (ARCHITECTURE.md §10).
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // The browser never holds a token: the Next.js route handlers keep the refresh token
                // in an encrypted HttpOnly cookie and attach the access token server-side
                // (ARCHITECTURE.md §10). Nothing reaching this filter chain is cookie-authenticated,
                // which is the only thing CSRF protection defends.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}
