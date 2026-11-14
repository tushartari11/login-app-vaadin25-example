package com.example.app.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserTest {

  /**
   * Reversible "hash" so tests stay fast and readable.
   */
  private static final PasswordHasher PLAIN_HASHER = new PasswordHasher() {
    @Override
    public String hash(String rawPassword) {
      return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
      return hash.equals(hash(rawPassword));
    }
  };

  private static User registerDuke(String password) {
    return User.register(new Username("duke"), new Email("Duke@Jakarta.EE"), password, Role.USER,
        PLAIN_HASHER);
  }

  @Test
  void shouldRegisterActiveUnlockedUserWithHashedPassword() {
    var user = registerDuke("Jakarta@11");

    assertTrue(user.canAuthenticate());
    assertTrue(user.id().isEmpty());
    assertEquals("hashed:Jakarta@11", user.passwordHash());
    assertTrue(user.passwordMatches("Jakarta@11", PLAIN_HASHER));
    assertFalse(user.passwordMatches("Jakarta@10", PLAIN_HASHER));
  }

  @Test
  void shouldNormaliseEmailToLowerCase() {
    assertEquals("duke@jakarta.ee", registerDuke("Jakarta@11").email().value());
  }

  @Test
  void shouldRejectPasswordWithoutSpecialCharacter() {
    var error = assertThrows(InvalidUserDataException.class, () -> registerDuke("Jakarta11"));

    assertEquals("Password must contain at least one special character", error.getMessage());
  }

  @Test
  void shouldRejectTooShortPassword() {
    assertThrows(InvalidUserDataException.class, () -> registerDuke("Ee@11"));
  }

  @Test
  void shouldRejectMalformedUsername() {
    assertThrows(InvalidUserDataException.class, () -> new Username("duke with spaces"));
    assertThrows(InvalidUserDataException.class, () -> new Username("jd"));
  }

  @Test
  void lockedUserShouldNotAuthenticateUntilUnlocked() {
    var user = registerDuke("Jakarta@11");

    user.lock();
    assertFalse(user.canAuthenticate());

    user.unlock();
    assertTrue(user.canAuthenticate());
  }

  @Test
  void shouldRecordLoginTime() {
    var user = registerDuke("Jakarta@11");
    assertTrue(user.lastLogin().isEmpty());

    user.recordLogin();

    assertTrue(user.lastLogin().isPresent());
  }
}
