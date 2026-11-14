package com.example.app.infrastructure.persistence;

import com.example.app.domain.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * JPA persistence model, deliberately separate from the domain {@code User}. Maps the pre-existing
 * {@code app_users} table unchanged, so existing data keeps working.
 */
@Entity
@Table(
    name = "app_users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_email", columnNames = "email")
    },
    indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_email", columnList = "email")
    })
@NamedQuery(name = UserEntity.FIND_BY_USERNAME,
    query = "SELECT u FROM UserEntity u WHERE u.username = :username")
@NamedQuery(name = UserEntity.COUNT_BY_USERNAME,
    query = "SELECT COUNT(u) FROM UserEntity u WHERE u.username = :username")
@NamedQuery(name = UserEntity.COUNT_BY_EMAIL,
    query = "SELECT COUNT(u) FROM UserEntity u WHERE LOWER(u.email) = LOWER(:email)")
@NamedQuery(name = UserEntity.FIND_ALL,
    query = "SELECT u FROM UserEntity u ORDER BY u.createdAt DESC")
@NamedQuery(name = UserEntity.COUNT_ALL,
    query = "SELECT COUNT(u) FROM UserEntity u")
public class UserEntity {

  static final String FIND_BY_USERNAME = "UserEntity.findByUsername";
  static final String COUNT_BY_USERNAME = "UserEntity.countByUsername";
  static final String COUNT_BY_EMAIL = "UserEntity.countByEmail";
  static final String FIND_ALL = "UserEntity.findAll";
  static final String COUNT_ALL = "UserEntity.countAll";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", nullable = false, unique = true, length = 50)
  private String username;

  @Column(name = "email", nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 60)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private Role role;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "account_locked", nullable = false)
  private boolean accountLocked;

  /**
   * Not modelled in the domain yet; kept so existing rows and the NOT NULL column stay valid.
   */
  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "last_login")
  private LocalDateTime lastLogin;

  @Version
  @Column(name = "version")
  private Long version;

  protected UserEntity() {
  }

  UserEntity(String username, LocalDateTime createdAt) {
    this.username = username;
    this.createdAt = createdAt;
  }

  @PrePersist
  @PreUpdate
  void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  Long getId() {
    return this.id;
  }

  String getUsername() {
    return this.username;
  }

  String getEmail() {
    return this.email;
  }

  void setEmail(String email) {
    this.email = email;
  }

  String getPasswordHash() {
    return this.passwordHash;
  }

  void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  Role getRole() {
    return this.role;
  }

  void setRole(Role role) {
    this.role = role;
  }

  boolean isActive() {
    return this.active;
  }

  void setActive(boolean active) {
    this.active = active;
  }

  boolean isAccountLocked() {
    return this.accountLocked;
  }

  void setAccountLocked(boolean accountLocked) {
    this.accountLocked = accountLocked;
  }

  LocalDateTime getCreatedAt() {
    return this.createdAt;
  }

  LocalDateTime getLastLogin() {
    return this.lastLogin;
  }

  void setLastLogin(LocalDateTime lastLogin) {
    this.lastLogin = lastLogin;
  }
}
