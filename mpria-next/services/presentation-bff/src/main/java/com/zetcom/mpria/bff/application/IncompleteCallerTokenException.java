package com.zetcom.mpria.bff.application;

/**
 * The access token is valid but does not carry the claims needed to act on the caller's behalf.
 *
 * <p>The tenant comes from the token and from nowhere else — not a parameter, not a header the
 * client controls — so a token without it cannot be substituted for. This is a 403, not a 401: the
 * caller authenticated successfully, the realm simply did not issue what this service needs.
 */
public class IncompleteCallerTokenException extends RuntimeException {

    public IncompleteCallerTokenException(String claim) {
        super("access token is missing a usable '" + claim + "' claim");
    }
}
