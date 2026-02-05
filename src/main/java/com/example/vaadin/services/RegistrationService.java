package com.example.vaadin.services;

import com.example.vaadin.entities.User;
import com.example.vaadin.security.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

/**
 * Service for user self-registration.
 * Applies default role and registration business rules.
 */
@ApplicationScoped
public class RegistrationService {

    private static final Role DEFAULT_ROLE = Role.USER;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 100;

    @Inject
    private UserService userService;

    @Transactional
    public User registerNewUser(String username, String email, String password) {
        validatePassword(password);
        return userService.registerUser(username, email, password, DEFAULT_ROLE);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException(
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long"
            );
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException(
                "Password must be at most " + MAX_PASSWORD_LENGTH + " characters long"
            );
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new ValidationException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new ValidationException("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            throw new ValidationException("Password must contain at least one digit");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new ValidationException("Password must contain at least one special character");
        }
    }

    public boolean isUsernameAvailable(String username) {
        return userService.findByUsername(username).isEmpty();
    }

    public boolean isEmailAvailable(String email) {
        return userService.findByEmail(email).isEmpty();
    }
}
