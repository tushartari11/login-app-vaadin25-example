package com.example.app.application.user;

import com.example.app.domain.user.InvalidUserDataException;
import com.example.app.domain.user.PasswordHasher;
import com.example.app.domain.user.User;
import com.example.app.domain.user.UserRepository;
import com.example.app.domain.user.Username;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Credential check behind the Jakarta Security identity store; records the login time on success.
 */
public class AuthenticateUserUseCase {

  private final UserRepository users;
  private final PasswordHasher hasher;

  @Inject
  public AuthenticateUserUseCase(UserRepository users, PasswordHasher hasher) {
    this.users = users;
    this.hasher = hasher;
  }

  @Transactional
  public Optional<AuthenticatedCaller> authenticate(String username, String rawPassword) {
    Optional<User> candidate;
    try {
      candidate = this.users.findByUsername(new Username(username));
    } catch (InvalidUserDataException malformedUsername) {
      // A name that could never have been registered is just a failed login, not an error.
      return Optional.empty();
    }

    var authenticated = candidate
        .filter(User::canAuthenticate)
        .filter(user -> user.passwordMatches(rawPassword, this.hasher));
    authenticated.ifPresent(this::recordLogin);
    return authenticated.map(
        user -> new AuthenticatedCaller(user.username().value(), Set.of(user.role().name())));
  }

  private void recordLogin(User user) {
    user.recordLogin();
    this.users.save(user);
  }
}
