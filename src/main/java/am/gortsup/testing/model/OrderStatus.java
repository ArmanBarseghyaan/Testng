package am.gortsup.testing.model;

/**
 * State machine statuses representing lifecycle stages of an Order.
 */
public enum OrderStatus {
    PENDING,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
