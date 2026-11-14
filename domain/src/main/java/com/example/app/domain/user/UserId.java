package com.example.app.domain.user;

import java.util.Objects;

/**
 * Identity of a persisted {@link User}; assigned by the database (identity column).
 */
public record UserId(Long value) {

  public UserId {
    Objects.requireNonNull(value, "value");
  }
}
