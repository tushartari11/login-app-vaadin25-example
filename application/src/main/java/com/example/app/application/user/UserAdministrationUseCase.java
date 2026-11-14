package com.example.app.application.user;

import com.example.app.domain.user.UserId;
import com.example.app.domain.user.UserRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Operations behind the admin screen. Authorization is enforced on the calling view
 * ({@code @RolesAllowed}).
 */
public class UserAdministrationUseCase {

  private final UserRepository users;

  @Inject
  public UserAdministrationUseCase(UserRepository users) {
    this.users = users;
  }

  public List<UserView> listUsers() {
    return this.users.findAll().stream()
        .map(UserViewMapper::toView)
        .toList();
  }

  @Transactional
  public void setAccountLocked(long userId, boolean locked) {
    var user = this.users.findById(new UserId(userId))
        .orElseThrow(() -> new UserNotFoundException(userId));
    if (locked) {
      user.lock();
    } else {
      user.unlock();
    }
    this.users.save(user);
  }
}
