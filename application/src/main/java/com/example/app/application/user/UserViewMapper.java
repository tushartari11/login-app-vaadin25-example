package com.example.app.application.user;

import com.example.app.domain.user.User;

/**
 * Kept out of {@link UserView} on purpose: a domain type in the DTO's API would leak into the UI
 * module, whose compile classpath excludes the domain.
 */
final class UserViewMapper {

  private UserViewMapper() {
  }

  static UserView toView(User user) {
    var id = user.id()
        .orElseThrow(() -> new IllegalStateException("Only persisted users can be shown"))
        .value();
    return new UserView(
        id,
        user.username().value(),
        user.email().value(),
        user.role().displayName(),
        user.isActive(),
        user.isAccountLocked(),
        user.createdAt());
  }
}
