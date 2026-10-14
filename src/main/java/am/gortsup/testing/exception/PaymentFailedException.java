package am.gortsup.testing.exception;

/**
 * Thrown when payment processing fails due to invalid card number, invalid amount, or declined transaction.
 */
public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super(message);
    }
}
