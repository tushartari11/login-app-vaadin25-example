package com.example.app.domain.user;

/**
 * Port: the hashing algorithm is an infrastructure detail.
 */
public interface PasswordHasher {

  String hash(String rawPassword);

  boolean matches(String rawPassword, String hash);
}
