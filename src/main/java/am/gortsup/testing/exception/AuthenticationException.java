package am.gortsup.testing.exception;

/**
 * Thrown when credentials fail authentication (invalid username or password).
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
