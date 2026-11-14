package com.example.app.infrastructure.persistence;

import com.example.app.domain.user.Email;
import com.example.app.domain.user.User;
import com.example.app.domain.user.UserId;
import com.example.app.domain.user.UserRepository;
import com.example.app.domain.user.Username;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

/**
 * Repository adapter: the only class that talks to JPA. Transactions are opened by the use cases.
 */
@ApplicationScoped
public class JpaUserRepository implements UserRepository {

  @PersistenceContext(unitName = "vaadinAuthPU")
  private EntityManager em;

  @Override
  public User save(User user) {
    var existingId = user.id();
    if (existingId.isEmpty()) {
      var entity = UserMapper.toNewEntity(user);
      this.em.persist(entity);
      // IDENTITY ids are only known after the INSERT has been flushed.
      this.em.flush();
      return UserMapper.toDomain(entity);
    }

    var id = existingId.get().value();
    var entity = this.em.find(UserEntity.class, id);
    if (entity == null) {
      throw new IllegalStateException("User " + id + " was deleted concurrently");
    }
    UserMapper.copyMutableState(user, entity);
    return UserMapper.toDomain(entity);
  }

  @Override
  public Optional<User> findById(UserId id) {
    return Optional.ofNullable(this.em.find(UserEntity.class, id.value()))
        .map(UserMapper::toDomain);
  }

  @Override
  public Optional<User> findByUsername(Username username) {
    return this.em.createNamedQuery(UserEntity.FIND_BY_USERNAME, UserEntity.class)
        .setParameter("username", username.value())
        .getResultStream()
        .findFirst()
        .map(UserMapper::toDomain);
  }

  @Override
  public boolean existsByUsername(Username username) {
    return this.em.createNamedQuery(UserEntity.COUNT_BY_USERNAME, Long.class)
        .setParameter("username", username.value())
        .getSingleResult() > 0;
  }

  @Override
  public boolean existsByEmail(Email email) {
    return this.em.createNamedQuery(UserEntity.COUNT_BY_EMAIL, Long.class)
        .setParameter("email", email.value())
        .getSingleResult() > 0;
  }

  @Override
  public List<User> findAll() {
    return this.em.createNamedQuery(UserEntity.FIND_ALL, UserEntity.class)
        .getResultStream()
        .map(UserMapper::toDomain)
        .toList();
  }

  @Override
  public long count() {
    return this.em.createNamedQuery(UserEntity.COUNT_ALL, Long.class).getSingleResult();
  }
}
