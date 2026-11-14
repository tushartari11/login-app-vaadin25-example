package com.example.app.infrastructure.security;

import com.example.app.application.user.AuthenticateUserUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

/**
 * Jakarta Security adapter: delegates the actual credential check to the application layer.
 */
@ApplicationScoped
public class DatabaseIdentityStore implements IdentityStore {

  @Inject
  private AuthenticateUserUseCase authenticateUser;

  @Override
  public CredentialValidationResult validate(Credential credential) {
    if (!(credential instanceof UsernamePasswordCredential usernamePassword)) {
      return CredentialValidationResult.NOT_VALIDATED_RESULT;
    }
    return this.authenticateUser
        .authenticate(usernamePassword.getCaller(), usernamePassword.getPasswordAsString())
        .map(caller -> new CredentialValidationResult(caller.username(), caller.groups()))
        .orElse(CredentialValidationResult.INVALID_RESULT);
  }
}
