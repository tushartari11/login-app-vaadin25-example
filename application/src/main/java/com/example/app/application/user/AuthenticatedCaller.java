package com.example.app.application.user;

import java.util.Set;

/**
 * Result of a successful credential check, shaped for a Jakarta Security
 * {@code CredentialValidationResult}.
 */
public record AuthenticatedCaller(String username, Set<String> groups) {

  public AuthenticatedCaller {
    groups = Set.copyOf(groups);
  }
}
