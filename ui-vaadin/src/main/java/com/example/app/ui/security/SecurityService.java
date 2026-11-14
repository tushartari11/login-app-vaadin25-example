package com.example.app.ui.security;

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;

import java.security.Principal;
import java.util.Optional;

/**
 * Programmatic login/logout for Vaadin views. Lives in the UI module because it needs the current
 * Vaadin servlet request; the credential check itself is in the application layer.
 */
@ApplicationScoped
public class SecurityService {

  @Inject
  private SecurityContext securityContext;

  public boolean authenticate(String username, String password) {
    var request = VaadinServletRequest.getCurrent().getHttpServletRequest();
    var response = VaadinServletResponse.getCurrent().getHttpServletResponse();
    var parameters = AuthenticationParameters.withParams()
        .credential(new UsernamePasswordCredential(username, password));

    return this.securityContext.authenticate(request, response, parameters)
        == AuthenticationStatus.SUCCESS;
  }

  /**
   * Invalidating the HTTP session also drops the identity remembered by {@code @AutoApplySession}.
   */
  public void logout() {
    var session = VaadinServletRequest.getCurrent().getHttpServletRequest().getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }

  public Optional<String> getAuthenticatedUsername() {
    return Optional.ofNullable(this.securityContext.getCallerPrincipal()).map(Principal::getName);
  }

  public boolean isAuthenticated() {
    return this.securityContext.getCallerPrincipal() != null;
  }

  /**
   * {@code role} is a Jakarta Security group name, e.g. "ADMIN" as used in {@code @RolesAllowed}.
   */
  public boolean isCallerInRole(String role) {
    return this.securityContext.isCallerInRole(role);
  }
}
