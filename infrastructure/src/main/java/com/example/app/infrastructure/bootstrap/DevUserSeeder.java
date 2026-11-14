package com.example.app.infrastructure.bootstrap;

import com.example.app.domain.user.Email;
import com.example.app.domain.user.PasswordHasher;
import com.example.app.domain.user.Role;
import com.example.app.domain.user.User;
import com.example.app.domain.user.UserRepository;
import com.example.app.domain.user.Username;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seeds one account per role into an empty database. FOR DEVELOPMENT ONLY: remove (or guard by
 * configuration) before going to production.
 */
@Singleton
@Startup
public class DevUserSeeder {

  private static final Logger LOGGER = LoggerFactory.getLogger(DevUserSeeder.class);

  private record SeedUser(String username, String email, String password, Role role) {

  }

  private static final List<SeedUser> SEED_USERS = List.of(
      new SeedUser("admin", "admin@example.com", "Admin@123", Role.ADMIN),
      new SeedUser("manager", "manager@example.com", "Manager@123", Role.MANAGER),
      new SeedUser("user", "user@example.com", "User@123", Role.USER),
      new SeedUser("guest", "guest@example.com", "Guest@123", Role.GUEST));

  @Inject
  private UserRepository users;

  @Inject
  private PasswordHasher hasher;

  /**
   * Runs in the singleton's container-managed transaction (REQUIRED by default).
   */
  @PostConstruct
  void seed() {
    if (this.users.count() > 0) {
      return;
    }
    LOGGER.info("Loading development users...");
    try {
      SEED_USERS.stream()
          .map(this::toUser)
          .forEach(this.users::save);
      LOGGER.info("Development users loaded");
    } catch (RuntimeException e) {
      // Seed data is a convenience: log and let the application start anyway.
      LOGGER.error("Failed to load development users", e);
    }
  }

  private User toUser(SeedUser seed) {
    return User.register(new Username(seed.username()), new Email(seed.email()), seed.password(),
        seed.role(), this.hasher);
  }
}
