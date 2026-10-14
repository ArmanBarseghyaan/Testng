package am.gortsup.testing.exception;

/**
 * Thrown when an operation is attempted on an account that has been locked.
 */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
