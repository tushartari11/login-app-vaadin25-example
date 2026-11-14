package com.example.app.application.user;

import java.time.LocalDateTime;

/**
 * Output DTO returned to any UI. The domain {@code User} never leaves the application layer.
 */
public record UserView(
    long id,
    String username,
    String email,
    String roleName,
    boolean active,
    boolean accountLocked,
    LocalDateTime createdAt) {

}
