package com.example.app.application.user;

public class UserAlreadyExistsException extends RuntimeException {

  private UserAlreadyExistsException(String message) {
    super(message);
  }

  static UserAlreadyExistsException forUsername(String username) {
    return new UserAlreadyExistsException("Username '" + username + "' is already taken");
  }

  static UserAlreadyExistsException forEmail(String email) {
    return new UserAlreadyExistsException("Email '" + email + "' is already registered");
  }
}
