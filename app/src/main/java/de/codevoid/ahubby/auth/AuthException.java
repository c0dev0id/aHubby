package de.codevoid.ahubby.auth;

import java.io.IOException;

/** Thrown when re-authentication fails and the session cannot be recovered. */
public class AuthException extends IOException {
    public AuthException(String message) {
        super(message);
    }
}
