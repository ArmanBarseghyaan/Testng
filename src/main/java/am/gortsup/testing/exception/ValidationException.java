package am.gortsup.testing.exception;

/**
 * Thrown when an input argument fails validation checks
 * (e.g., null or blank string, invalid email format, negative quantity, invalid range).
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
