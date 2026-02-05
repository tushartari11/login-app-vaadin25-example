package com.example.vaadin.security;

import com.example.vaadin.entities.User;
import com.example.vaadin.repositories.UserRepository;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;
import java.util.Set;

/**
 * Service for authentication operations and security context access.
 * Provides programmatic login/logout and current user retrieval.
 */
@ApplicationScoped
public class SecurityService {

    @Inject
    private SecurityContext securityContext;

    @Inject
    private UserRepository userRepository;

    /**
     * Authenticates user with username and password.
     *
     * @return true if authentication successful
     */
    public boolean authenticate(String username, String password) {
        HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();
        HttpServletResponse response = VaadinServletResponse.getCurrent().getHttpServletResponse();

        Credential credential = new UsernamePasswordCredential(username, password);
        AuthenticationStatus status = securityContext.authenticate(
            request,
            response,
            AuthenticationParameters.withParams().credential(credential)
        );

        return status == AuthenticationStatus.SUCCESS;
    }

    /**
     * Logs out the current user by invalidating the HTTP session.
     * Session invalidation clears the identity stored by @AutoApplySession.
     */
    public void logout() {
        HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * Gets the currently authenticated username.
     */
    public Optional<String> getAuthenticatedUsername() {
        if (securityContext.getCallerPrincipal() == null) {
            return Optional.empty();
        }
        return Optional.of(securityContext.getCallerPrincipal().getName());
    }

    /**
     * Gets the currently authenticated user entity.
     */
    public Optional<User> getAuthenticatedUser() {
        return getAuthenticatedUsername()
            .flatMap(userRepository::findByUsername);
    }

    /**
     * Checks if a user is authenticated.
     */
    public boolean isAuthenticated() {
        return securityContext.getCallerPrincipal() != null;
    }

    /**
     * Checks if current user has a specific role.
     */
    public boolean hasRole(Role role) {
        return securityContext.isCallerInRole(role.name());
    }

    /**
     * Checks if current user has any of the specified roles.
     */
    public boolean hasAnyRole(Role... roles) {
        return Set.of(roles).stream()
            .anyMatch(role -> securityContext.isCallerInRole(role.name()));
    }

    /**
     * Checks if current user has all of the specified roles.
     */
    public boolean hasAllRoles(Role... roles) {
        return Set.of(roles).stream()
            .allMatch(role -> securityContext.isCallerInRole(role.name()));
    }
}
