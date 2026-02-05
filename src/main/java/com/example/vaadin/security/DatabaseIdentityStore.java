package com.example.vaadin.security;

import com.example.vaadin.entities.User;
import com.example.vaadin.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

import java.util.Set;

import static jakarta.security.enterprise.identitystore.CredentialValidationResult.*;

/**
 * Custom IdentityStore that validates credentials against PostgreSQL database.
 * Integrates with Jakarta Security authentication mechanism.
 */
@ApplicationScoped
public class DatabaseIdentityStore implements IdentityStore {

    @Inject
    private UserRepository userRepository;

    @Inject
    private PasswordHashService passwordHashService;

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (!(credential instanceof UsernamePasswordCredential)) {
            return NOT_VALIDATED_RESULT;
        }

        var usernamePassword = (UsernamePasswordCredential) credential;
        String username = usernamePassword.getCaller();
        String password = usernamePassword.getPasswordAsString();

        return userRepository.findByUsername(username)
            .filter(User::canAuthenticate)
            .filter(user -> passwordHashService.verifyPassword(password, user.getPasswordHash()))
            .map(user -> new CredentialValidationResult(
                user.getUsername(),
                Set.of(user.getRole().name())
            ))
            .orElse(INVALID_RESULT);
    }
}
