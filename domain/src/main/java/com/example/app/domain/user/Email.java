package com.example.app.domain.user;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A validated e-mail address, normalised to lower case so uniqueness checks are case-insensitive.
 */
public record Email(String value) {

  private static final int MAX_LENGTH = 100;
  private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  public Email {
    var normalised = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    if (normalised.length() > MAX_LENGTH || !FORMAT.matcher(normalised).matches()) {
      throw new InvalidUserDataException(
          "Please enter a valid email address (at most " + MAX_LENGTH + " characters)");
    }
    value = normalised;
  }
}
