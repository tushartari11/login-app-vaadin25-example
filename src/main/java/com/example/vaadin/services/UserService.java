package com.example.vaadin.services;

import com.example.vaadin.entities.User;
import com.example.vaadin.repositories.UserRepository;
import com.example.vaadin.security.PasswordHashService;
import com.example.vaadin.security.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

import java.util.List;
import java.util.Optional;

/**
 * Service for user management operations.
 * Contains business logic for user creation, updates, and validation.
 */
@ApplicationScoped
public class UserService {

    @Inject
    private UserRepository userRepository;

    @Inject
    private PasswordHashService passwordHashService;

    @Transactional
    public User registerUser(String username, String email, String password, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new ValidationException("Username '" + username + "' is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email '" + email + "' is already registered");
        }

        String passwordHash = passwordHashService.hashPassword(password);
        User user = User.createNew(username, email, passwordHash, role);

        return userRepository.create(user);
    }

    @Transactional
    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newHash = passwordHashService.hashPassword(newPassword);
        user.setPasswordHash(newHash);

        userRepository.update(user);
    }

    @Transactional
    public User updateProfile(Long userId, String email) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getEmail().equalsIgnoreCase(email)) {
            if (userRepository.existsByEmail(email)) {
                throw new ValidationException("Email is already in use");
            }
            user.setEmail(email);
            user.setEmailVerified(false);
        }

        return userRepository.update(user);
    }

    @Transactional
    public User updateRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(newRole);
        return userRepository.update(user);
    }

    @Transactional
    public void setAccountLocked(Long userId, boolean locked) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setAccountLocked(locked);
        userRepository.update(user);
    }

    @Transactional
    public void setActive(Long userId, boolean active) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setActive(active);
        userRepository.update(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<User> findActiveUsers() {
        return userRepository.findAllActive();
    }

    public long getUserCount() {
        return userRepository.count();
    }
}
