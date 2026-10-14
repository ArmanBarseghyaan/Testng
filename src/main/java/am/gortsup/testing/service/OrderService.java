package am.gortsup.testing.service;

import am.gortsup.testing.exception.IllegalOrderStateException;
import am.gortsup.testing.exception.PaymentFailedException;
import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.*;
import am.gortsup.testing.util.MathAndFinanceUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service managing customer order lifecycle, payment processing, shipping, and cancellations.
 */
public class OrderService {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    /**
     * Creates an order from cart items.
     *
     * @param userId customer identifier
     * @param items non-empty list of items
     * @param shippingAddress destination address
     * @param taxRatePercentage tax rate percentage (0.0 to 100.0)
     * @param coupon optional coupon applied to order
     * @return created Order in PENDING status
     * @throws ValidationException if arguments are invalid or items list is empty
     */
    public Order createOrder(String userId, List<CartItem> items, Address shippingAddress,
                             double taxRatePercentage, Coupon coupon) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new ValidationException("User ID cannot be null or empty");
        }
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Order items cannot be empty");
        }
        if (shippingAddress == null) {
            throw new ValidationException("Shipping address cannot be null");
        }
        if (taxRatePercentage < 0.0 || taxRatePercentage > 100.0) {
            throw new ValidationException("Tax rate must be between 0.0 and 100.0");
        }

        double subtotal = 0.0;
        for (CartItem item : items) {
            subtotal += item.getSubtotal();
        }
        subtotal = MathAndFinanceUtils.roundToDecimalPlaces(subtotal, 2);

        double discount = 0.0;
        if (coupon != null && subtotal >= coupon.getMinOrderAmount()) {
            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
                discount = subtotal * (coupon.getDiscountValue() / 100.0);
            } else {
                discount = Math.min(subtotal, coupon.getDiscountValue());
            }
            coupon.incrementTimesUsed();
        }
        discount = MathAndFinanceUtils.roundToDecimalPlaces(discount, 2);

        double taxable = Math.max(0.0, subtotal - discount);
        double tax = MathAndFinanceUtils.calculateTax(taxable, taxRatePercentage);
        double total = MathAndFinanceUtils.roundToDecimalPlaces(taxable + tax, 2);

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(orderId, userId, items, subtotal, discount, tax, total, shippingAddress);
        orders.put(orderId, order);
        return order;
    }

    /**
     * Processes payment for a pending order using credit card.
     *
     * @param orderId order identifier
     * @param cardNumber credit card number (must pass Luhn algorithm check)
     * @param amount payment amount (must exactly match order total)
     * @throws ResourceNotFoundException if order does not exist
     * @throws IllegalOrderStateException if order is not in PENDING status
     * @throws PaymentFailedException if card fails Luhn check or amount does not match
     */
    public void payOrder(String orderId, String cardNumber, double amount) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalOrderStateException("Cannot pay for order in status: " + order.getStatus());
        }
        if (!MathAndFinanceUtils.isValidLuhn(cardNumber)) {
            throw new PaymentFailedException("Invalid credit card number: Luhn check failed");
        }
        double roundedAmount = MathAndFinanceUtils.roundToDecimalPlaces(amount, 2);
        if (Double.compare(roundedAmount, order.getTotal()) != 0) {
            throw new PaymentFailedException("Payment amount mismatch. Expected: " + order.getTotal() + ", received: " + roundedAmount);
        }

        order.setStatus(OrderStatus.PAID);
    }

    /**
     * Dispatches an order for delivery.
     *
     * @param orderId order identifier
     * @param trackingNumber non-empty tracking code
     * @throws ResourceNotFoundException if order does not exist
     * @throws ValidationException if trackingNumber is null/empty
     * @throws IllegalOrderStateException if order is not in PAID or PROCESSING status
     */
    public void shipOrder(String orderId, String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            throw new ValidationException("Tracking number cannot be empty");
        }
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PROCESSING) {
            throw new IllegalOrderStateException("Cannot ship order in status: " + order.getStatus());
        }
        order.setTrackingNumber(trackingNumber.trim());
        order.setStatus(OrderStatus.SHIPPED);
    }

    /**
     * Marks an in-transit order as delivered to the customer.
     *
     * @param orderId order identifier
     * @throws ResourceNotFoundException if order does not exist
     * @throws IllegalOrderStateException if order status is not SHIPPED
     */
    public void deliverOrder(String orderId) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalOrderStateException("Cannot deliver an order that is not currently SHIPPED (current status: " + order.getStatus() + ")");
        }
        order.setStatus(OrderStatus.DELIVERED);
    }

    /**
     * Cancels an order. Orders can only be cancelled before they are shipped.
     *
     * @param orderId order identifier
     * @throws ResourceNotFoundException if order does not exist
     * @throws IllegalOrderStateException if order is already SHIPPED, DELIVERED, or CANCELLED
     */
    public void cancelOrder(String orderId) {
        Order order = getOrder(orderId);
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalOrderStateException("Cannot cancel an order that has already been shipped or delivered");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalOrderStateException("Order is already cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
    }

    /**
     * Issues a refund for an order.
     *
     * @param orderId order identifier
     * @param refundAmount amount to refund (must be > 0 and <= order total)
     * @throws ResourceNotFoundException if order does not exist
     * @throws IllegalOrderStateException if order was never paid
     * @throws ValidationException if refundAmount is <= 0 or exceeds order total
     */
    public void refundOrder(String orderId, double refundAmount) {
        Order order = getOrder(orderId);
        if (order.getStatus() == OrderStatus.PENDING) {
            throw new IllegalOrderStateException("Cannot refund an order that was never paid");
        }
        if (refundAmount <= 0.0) {
            throw new ValidationException("Refund amount must be strictly positive");
        }
        double roundedRefund = MathAndFinanceUtils.roundToDecimalPlaces(refundAmount, 2);
        if (roundedRefund > order.getTotal()) {
            throw new ValidationException("Refund amount (" + roundedRefund + ") cannot exceed order total (" + order.getTotal() + ")");
        }
        order.setStatus(OrderStatus.REFUNDED);
    }

    /**
     * Retrieves an order by its ID.
     *
     * @param orderId order ID
     * @return Order entity
     * @throws ResourceNotFoundException if not found
     */
    public Order getOrder(String orderId) {
        if (orderId == null) {
            throw new ResourceNotFoundException("Order ID cannot be null");
        }
        Order order = orders.get(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found with ID: " + orderId);
        }
        return order;
    }

    /**
     * Retrieves all orders placed by a specific user.
     *
     * @param userId user identifier
     * @return list of orders placed by this user
     */
    public List<Order> getOrdersByUser(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return orders.values().stream()
                .filter(o -> userId.equals(o.getUserId()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all orders with a given status.
     *
     * @param status order status filter
     * @return matching orders
     */
    public List<Order> getOrdersByStatus(OrderStatus status) {
        if (status == null) {
            return Collections.emptyList();
        }
        return orders.values().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }
}
