package com.example.app.domain.user;

import java.util.List;
import java.util.Optional;

/**
 * Port: implemented in infrastructure (JPA today; Jakarta Data or JDBC would also fit).
 */
public interface UserRepository {

  /**
   * Inserts a new user or updates an existing one; the returned user always carries its id.
   */
  User save(User user);

  Optional<User> findById(UserId id);

  Optional<User> findByUsername(Username username);

  boolean existsByUsername(Username username);

  boolean existsByEmail(Email email);

  /**
   * Newest first.
   */
  List<User> findAll();

  long count();
}
