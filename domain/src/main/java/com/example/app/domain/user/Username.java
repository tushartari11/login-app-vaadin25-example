package com.example.app.domain.user;

import java.util.regex.Pattern;

/**
 * Login name. Case-sensitive on purpose: "Duke" and "duke" are different callers.
 */
public record Username(String value) {

  private static final Pattern FORMAT = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");

  public Username {
    if (value == null || !FORMAT.matcher(value).matches()) {
      throw new InvalidUserDataException(
          "Username must be 3-50 characters and contain only letters, numbers, underscores, and hyphens");
    }
  }
}
