package com.example.app.domain.user;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root. Plain Java: no JPA, no CDI, no Vaadin.
 */
public final class User {

  /**
   * {@code null} until the repository has persisted the user.
   */
  private final UserId id;
  private final Username username;
  private final Email email;
  private final String passwordHash;
  private final Role role;
  private final boolean active;
  private final LocalDateTime createdAt;
  private boolean accountLocked;
  private LocalDateTime lastLogin;

  private User(Builder builder) {
    this.id = builder.id;
    this.username = Objects.requireNonNull(builder.username, "username");
    this.email = Objects.requireNonNull(builder.email, "email");
    this.passwordHash = Objects.requireNonNull(builder.passwordHash, "passwordHash");
    this.role = Objects.requireNonNull(builder.role, "role");
    this.active = builder.active;
    this.accountLocked = builder.accountLocked;
    this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt");
    this.lastLogin = builder.lastLogin;
  }

  /**
   * Factory for a new account: enforces the {@link PasswordPolicy} before the password is hashed.
   */
  public static User register(Username username, Email email, String rawPassword, Role role,
      PasswordHasher hasher) {
    PasswordPolicy.validate(rawPassword);
    var builder = new Builder();
    builder.username = username;
    builder.email = email;
    builder.passwordHash = hasher.hash(rawPassword);
    builder.role = role;
    builder.active = true;
    builder.createdAt = LocalDateTime.now();
    return new User(builder);
  }

  /**
   * Rehydration from persistence: used only by repository adapters.
   */
  public static Builder restore(UserId id) {
    var builder = new Builder();
    builder.id = Objects.requireNonNull(id, "id");
    return builder;
  }

  public boolean canAuthenticate() {
    return this.active && !this.accountLocked;
  }

  public boolean passwordMatches(String rawPassword, PasswordHasher hasher) {
    return rawPassword != null && hasher.matches(rawPassword, this.passwordHash);
  }

  public void recordLogin() {
    this.lastLogin = LocalDateTime.now();
  }

  public void lock() {
    this.accountLocked = true;
  }

  public void unlock() {
    this.accountLocked = false;
  }

  public Optional<UserId> id() {
    return Optional.ofNullable(this.id);
  }

  public Username username() {
    return this.username;
  }

  public Email email() {
    return this.email;
  }

  public String passwordHash() {
    return this.passwordHash;
  }

  public Role role() {
    return this.role;
  }

  public boolean isActive() {
    return this.active;
  }

  public boolean isAccountLocked() {
    return this.accountLocked;
  }

  public LocalDateTime createdAt() {
    return this.createdAt;
  }

  public Optional<LocalDateTime> lastLogin() {
    return Optional.ofNullable(this.lastLogin);
  }

  /**
   * Collects persisted state for {@link #restore(UserId)}; keeps the rehydration call readable.
   */
  public static final class Builder {

    private UserId id;
    private Username username;
    private Email email;
    private String passwordHash;
    private Role role;
    private boolean active;
    private boolean accountLocked;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    private Builder() {
    }

    public Builder username(Username username) {
      this.username = username;
      return this;
    }

    public Builder email(Email email) {
      this.email = email;
      return this;
    }

    public Builder passwordHash(String passwordHash) {
      this.passwordHash = passwordHash;
      return this;
    }

    public Builder role(Role role) {
      this.role = role;
      return this;
    }

    public Builder active(boolean active) {
      this.active = active;
      return this;
    }

    public Builder accountLocked(boolean accountLocked) {
      this.accountLocked = accountLocked;
      return this;
    }

    public Builder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder lastLogin(LocalDateTime lastLogin) {
      this.lastLogin = lastLogin;
      return this;
    }

    public User build() {
      return new User(this);
    }
  }
}
