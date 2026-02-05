package com.example.vaadin.repositories;

import com.example.vaadin.entities.User;
import com.example.vaadin.security.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity data access.
 * All methods are transactional and use JPA EntityManager.
 */
@ApplicationScoped
public class UserRepository {

    @PersistenceContext(unitName = "vaadinAuthPU")
    private EntityManager em;

    @Transactional
    public User create(User user) {
        em.persist(user);
        return user;
    }

    @Transactional
    public User update(User user) {
        return em.merge(user);
    }

    @Transactional
    public void delete(User user) {
        if (!em.contains(user)) {
            user = em.merge(user);
        }
        em.remove(user);
    }

    public Optional<User> findById(Long id) {
        User user = em.find(User.class, id);
        return Optional.ofNullable(user);
    }

    public Optional<User> findByUsername(String username) {
        TypedQuery<User> query = em.createQuery(
            "SELECT u FROM User u WHERE u.username = :username",
            User.class
        );
        query.setParameter("username", username);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        TypedQuery<User> query = em.createQuery(
            "SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)",
            User.class
        );
        query.setParameter("email", email);
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public boolean existsByUsername(String username) {
        Long count = em.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.username = :username",
            Long.class
        )
        .setParameter("username", username)
        .getSingleResult();
        return count > 0;
    }

    public boolean existsByEmail(String email) {
        Long count = em.createQuery(
            "SELECT COUNT(u) FROM User u WHERE LOWER(u.email) = LOWER(:email)",
            Long.class
        )
        .setParameter("email", email)
        .getSingleResult();
        return count > 0;
    }

    public List<User> findAll() {
        return em.createQuery("SELECT u FROM User u ORDER BY u.createdAt DESC", User.class)
                 .getResultList();
    }

    public List<User> findByRole(Role role) {
        return em.createQuery(
            "SELECT u FROM User u WHERE u.role = :role ORDER BY u.username",
            User.class
        )
        .setParameter("role", role)
        .getResultList();
    }

    public List<User> findAllActive() {
        return em.createQuery(
            "SELECT u FROM User u WHERE u.active = true AND u.accountLocked = false ORDER BY u.username",
            User.class
        )
        .getResultList();
    }

    public long count() {
        return em.createQuery("SELECT COUNT(u) FROM User u", Long.class)
                 .getSingleResult();
    }
}
