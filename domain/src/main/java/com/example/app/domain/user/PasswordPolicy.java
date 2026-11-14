package com.example.app.domain.user;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Password strength rules, applied to every new or changed password regardless of which UI submits
 * it.
 */
public final class PasswordPolicy {

  public static final int MIN_LENGTH = 8;
  public static final int MAX_LENGTH = 100;

  private record Rule(Predicate<String> isSatisfied, String message) {

  }

  private static final List<Rule> CHARACTER_RULES = List.of(
      new Rule(Pattern.compile("[A-Z]").asPredicate(),
          "Password must contain at least one uppercase letter"),
      new Rule(Pattern.compile("[a-z]").asPredicate(),
          "Password must contain at least one lowercase letter"),
      new Rule(Pattern.compile("\\d").asPredicate(), "Password must contain at least one digit"),
      new Rule(Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]").asPredicate(),
          "Password must contain at least one special character"));

  private PasswordPolicy() {
  }

  public static void validate(String rawPassword) {
    if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
      throw new InvalidUserDataException(
          "Password must be at least " + MIN_LENGTH + " characters long");
    }
    if (rawPassword.length() > MAX_LENGTH) {
      throw new InvalidUserDataException(
          "Password must be at most " + MAX_LENGTH + " characters long");
    }
    CHARACTER_RULES.stream()
        .filter(rule -> !rule.isSatisfied().test(rawPassword))
        .findFirst()
        .ifPresent(rule -> {
          throw new InvalidUserDataException(rule.message());
        });
  }
}
