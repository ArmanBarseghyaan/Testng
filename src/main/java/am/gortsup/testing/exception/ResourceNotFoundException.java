package am.gortsup.testing.exception;

/**
 * Thrown when an entity (User, Product, Order, Cart) is requested but does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
