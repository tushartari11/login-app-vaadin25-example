package com.example.vaadin.security;

import jakarta.enterprise.context.ApplicationScoped;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Service for secure password hashing and verification using BCrypt.
 * BCrypt automatically handles salting and is resistant to rainbow table attacks.
 */
@ApplicationScoped
public class PasswordHashService {

    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Hashes a plain-text password using BCrypt.
     *
     * @param plainTextPassword the password to hash
     * @return the BCrypt hash (60 characters)
     * @throws IllegalArgumentException if password is null or empty
     */
    public String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verifies a plain-text password against a BCrypt hash.
     *
     * @param plainTextPassword the password to verify
     * @param hashedPassword the stored BCrypt hash
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
