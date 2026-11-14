package com.example.app.application.user;

import com.example.app.domain.user.Email;
import com.example.app.domain.user.PasswordHasher;
import com.example.app.domain.user.Role;
import com.example.app.domain.user.User;
import com.example.app.domain.user.UserRepository;
import com.example.app.domain.user.Username;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Self-registration. Only portable annotations (jakarta.inject / jakarta.transaction), so it runs
 * unchanged under CDI (Payara), Spring and Quarkus. Discovered via META-INF/beans.xml (mode
 * "all").
 */
public class RegisterUserUseCase {

  private static final Role SELF_REGISTERED_ROLE = Role.USER;

  private final UserRepository users;
  private final PasswordHasher hasher;

  @Inject
  public RegisterUserUseCase(UserRepository users, PasswordHasher hasher) {
    this.users = users;
    this.hasher = hasher;
  }

  /**
   * @throws UserAlreadyExistsException if the username or e-mail is taken
   * @throws IllegalArgumentException   if a field breaks a domain rule (format, password strength)
   */
  @Transactional
  public UserView register(RegisterUserCommand command) {
    var username = new Username(command.username());
    var email = new Email(command.email());
    if (this.users.existsByUsername(username)) {
      throw UserAlreadyExistsException.forUsername(username.value());
    }
    if (this.users.existsByEmail(email)) {
      throw UserAlreadyExistsException.forEmail(email.value());
    }
    var user = User.register(username, email, command.password(), SELF_REGISTERED_ROLE,
        this.hasher);
    return UserViewMapper.toView(this.users.save(user));
  }

  /**
   * Expects a well-formed username; format is checked by the caller's field validators first.
   */
  public boolean isUsernameAvailable(String username) {
    return !this.users.existsByUsername(new Username(username));
  }

  /**
   * Expects a well-formed e-mail; format is checked by the caller's field validators first.
   */
  public boolean isEmailAvailable(String email) {
    return !this.users.existsByEmail(new Email(email));
  }
}
