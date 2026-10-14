package am.gortsup.testing.exception;

/**
 * Thrown when a coupon is expired, inactive, usage limit exceeded, or minimum order amount not met.
 */
public class InvalidCouponException extends RuntimeException {
    public InvalidCouponException(String message) {
        super(message);
    }
}
