package com.example.app.infrastructure.persistence;

import com.example.app.domain.user.Email;
import com.example.app.domain.user.User;
import com.example.app.domain.user.UserId;
import com.example.app.domain.user.Username;

final class UserMapper {

  private UserMapper() {
  }

  static UserEntity toNewEntity(User user) {
    var entity = new UserEntity(user.username().value(), user.createdAt());
    copyMutableState(user, entity);
    return entity;
  }

  /**
   * Copies only what the domain owns; id, version and email_verified stay under JPA's control.
   */
  static void copyMutableState(User user, UserEntity entity) {
    entity.setEmail(user.email().value());
    entity.setPasswordHash(user.passwordHash());
    entity.setRole(user.role());
    entity.setActive(user.isActive());
    entity.setAccountLocked(user.isAccountLocked());
    entity.setLastLogin(user.lastLogin().orElse(null));
  }

  static User toDomain(UserEntity entity) {
    return User.restore(new UserId(entity.getId()))
        .username(new Username(entity.getUsername()))
        .email(new Email(entity.getEmail()))
        .passwordHash(entity.getPasswordHash())
        .role(entity.getRole())
        .active(entity.isActive())
        .accountLocked(entity.isAccountLocked())
        .createdAt(entity.getCreatedAt())
        .lastLogin(entity.getLastLogin())
        .build();
  }
}
