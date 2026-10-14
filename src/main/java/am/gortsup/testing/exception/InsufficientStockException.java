package am.gortsup.testing.exception;

/**
 * Thrown when an inventory deduction or reservation exceeds available stock.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
