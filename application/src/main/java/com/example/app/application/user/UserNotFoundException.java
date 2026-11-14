package com.example.app.application.user;

public class UserNotFoundException extends RuntimeException {

  UserNotFoundException(long userId) {
    super("User " + userId + " not found");
  }
}
