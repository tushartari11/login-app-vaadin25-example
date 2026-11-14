package com.example.app.application.user;

/**
 * Input of the registration use case: plain data, no domain types.
 */
public record RegisterUserCommand(String username, String email, String password) {

}
