package com.example.app.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.app.domain.user.PasswordHasher;
import com.example.app.domain.user.Username;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserUseCasesTest {

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

  private static final RegisterUserCommand DUKE = new RegisterUserCommand("duke", "duke@jakarta.ee",
      "Jakarta@11");

  private InMemoryUserRepository users;
  private RegisterUserUseCase registerUser;
  private AuthenticateUserUseCase authenticateUser;
  private UserAdministrationUseCase administration;

  @BeforeEach
  void setUp() {
    this.users = new InMemoryUserRepository();
    this.registerUser = new RegisterUserUseCase(this.users, PLAIN_HASHER);
    this.authenticateUser = new AuthenticateUserUseCase(this.users, PLAIN_HASHER);
    this.administration = new UserAdministrationUseCase(this.users);
  }

  @Test
  void shouldRegisterSelfServiceUsersWithUserRole() {
    var view = this.registerUser.register(DUKE);

    assertEquals("duke", view.username());
    assertEquals("User", view.roleName());
    assertTrue(view.active());
    assertFalse(this.registerUser.isUsernameAvailable("duke"));
    assertFalse(this.registerUser.isEmailAvailable("DUKE@jakarta.ee"));
  }

  @Test
  void shouldRejectDuplicateUsername() {
    this.registerUser.register(DUKE);
    var sameName = new RegisterUserCommand("duke", "other@jakarta.ee", "Jakarta@11");

    var error = assertThrows(UserAlreadyExistsException.class,
        () -> this.registerUser.register(sameName));

    assertEquals("Username 'duke' is already taken", error.getMessage());
  }

  @Test
  void shouldRejectDuplicateEmailIgnoringCase() {
    this.registerUser.register(DUKE);
    var sameEmail = new RegisterUserCommand("moby", "Duke@Jakarta.EE", "Jakarta@11");

    assertThrows(UserAlreadyExistsException.class, () -> this.registerUser.register(sameEmail));
  }

  @Test
  void shouldAuthenticateWithRoleAsGroupAndRecordLogin() {
    this.registerUser.register(DUKE);

    var caller = this.authenticateUser.authenticate("duke", "Jakarta@11");

    assertEquals(Set.of("USER"), caller.orElseThrow().groups());
    assertTrue(
        this.users.findByUsername(new Username("duke")).orElseThrow().lastLogin().isPresent());
  }

  @Test
  void shouldNotAuthenticateWithWrongPasswordOrMalformedUsername() {
    this.registerUser.register(DUKE);

    assertTrue(this.authenticateUser.authenticate("duke", "wrong").isEmpty());
    assertTrue(this.authenticateUser.authenticate("no such user!", "Jakarta@11").isEmpty());
  }

  @Test
  void lockedAccountShouldNotAuthenticate() {
    var duke = this.registerUser.register(DUKE);

    this.administration.setAccountLocked(duke.id(), true);

    assertTrue(this.authenticateUser.authenticate("duke", "Jakarta@11").isEmpty());
    assertTrue(this.administration.listUsers().getFirst().accountLocked());
  }

  @Test
  void lockingUnknownUserShouldFail() {
    assertThrows(UserNotFoundException.class, () -> this.administration.setAccountLocked(42, true));
  }
}
