package com.example.app.domain.user;

/**
 * A domain rule was violated (bad username, e-mail or weak password). Extends
 * {@link IllegalArgumentException} so outer layers can react without importing domain types.
 */
public class InvalidUserDataException extends IllegalArgumentException {

  public InvalidUserDataException(String message) {
    super(message);
  }
}
