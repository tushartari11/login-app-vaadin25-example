package com.example.app.application.user;

import com.example.app.domain.user.Email;
import com.example.app.domain.user.User;
import com.example.app.domain.user.UserId;
import com.example.app.domain.user.UserRepository;
import com.example.app.domain.user.Username;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test double for the repository port; assigns ids the way an identity column would.
 */
class InMemoryUserRepository implements UserRepository {

  private final Map<Long, User> users = new LinkedHashMap<>();
  private long nextId = 1;

  @Override
  public User save(User user) {
    var id = user.id().orElseGet(() -> new UserId(this.nextId++));
    var stored = User.restore(id)
        .username(user.username())
        .email(user.email())
        .passwordHash(user.passwordHash())
        .role(user.role())
        .active(user.isActive())
        .accountLocked(user.isAccountLocked())
        .createdAt(user.createdAt())
        .lastLogin(user.lastLogin().orElse(null))
        .build();
    this.users.put(id.value(), stored);
    return stored;
  }

  @Override
  public Optional<User> findById(UserId id) {
    return Optional.ofNullable(this.users.get(id.value()));
  }

  @Override
  public Optional<User> findByUsername(Username username) {
    return this.users.values().stream().filter(u -> u.username().equals(username)).findFirst();
  }

  @Override
  public boolean existsByUsername(Username username) {
    return findByUsername(username).isPresent();
  }

  @Override
  public boolean existsByEmail(Email email) {
    return this.users.values().stream().anyMatch(u -> u.email().equals(email));
  }

  @Override
  public List<User> findAll() {
    return new ArrayList<>(this.users.values()).reversed();
  }

  @Override
  public long count() {
    return this.users.size();
  }
}
