package am.gortsup.testing.exception;

/**
 * Thrown when an order operation violates order lifecycle rules
 * (e.g., trying to ship an unpaid order, cancel an already shipped/delivered order).
 */
public class IllegalOrderStateException extends RuntimeException {
    public IllegalOrderStateException(String message) {
        super(message);
    }
}
