package com.example.app.infrastructure.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategy;
import com.example.app.domain.user.PasswordHasher;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * bcrypt via at.favre.lib (maintained replacement for org.mindrot:jbcrypt). Uses version $2a$ and
 * truncates passwords beyond bcrypt's 72-byte limit, exactly like jbcrypt did, so hashes created by
 * the previous implementation still verify.
 */
@ApplicationScoped
public class BcryptPasswordHasher implements PasswordHasher {

  private static final int COST = 12;
  private static final BCrypt.Version VERSION = BCrypt.Version.VERSION_2A;
  private static final LongPasswordStrategy JBCRYPT_COMPATIBLE = LongPasswordStrategies.truncate(
      VERSION);

  @Override
  public String hash(String rawPassword) {
    return BCrypt.with(VERSION, JBCRYPT_COMPATIBLE).hashToString(COST, rawPassword.toCharArray());
  }

  @Override
  public boolean matches(String rawPassword, String hash) {
    if (rawPassword == null || hash == null) {
      return false;
    }
    return BCrypt.verifyer(VERSION, JBCRYPT_COMPATIBLE)
        .verify(rawPassword.toCharArray(), hash)
        .verified;
  }
}
