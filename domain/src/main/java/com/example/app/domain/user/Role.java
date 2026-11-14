package com.example.app.domain.user;

/**
 * Application roles, ordered from least to most privileged. The constant names are the Jakarta
 * Security group names.
 */
public enum Role {
  GUEST("Guest"),
  USER("User"),
  MANAGER("Manager"),
  ADMIN("Administrator");

  private final String displayName;

  Role(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return this.displayName;
  }

  public boolean hasPrivilege(Role requiredRole) {
    return this.ordinal() >= requiredRole.ordinal();
  }
}
