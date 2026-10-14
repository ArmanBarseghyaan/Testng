package am.gortsup.testing.service;

import am.gortsup.testing.exception.InsufficientStockException;
import am.gortsup.testing.exception.ValidationException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing product stock quantities, reservations, and inventory replenishment.
 */
public class InventoryService {

    private final Map<String, Integer> availableStock = new ConcurrentHashMap<>();
    private final Map<String, Integer> reservedStock = new ConcurrentHashMap<>();

    /**
     * Initializes or overrides the available stock quantity for a product.
     *
     * @param productId product identifier
     * @param quantity non-negative stock count
     * @throws ValidationException if productId is null/empty or quantity < 0
     */
    public void setStock(String productId, int quantity) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new ValidationException("Product ID cannot be null or empty");
        }
        if (quantity < 0) {
            throw new ValidationException("Stock quantity cannot be negative: " + quantity);
        }
        availableStock.put(productId, quantity);
        reservedStock.putIfAbsent(productId, 0);
    }

    /**
     * Retrieves currently available (unreserved) stock for a product.
     *
     * @param productId product identifier
     * @return available stock count (0 if not tracked)
     */
    public int getStock(String productId) {
        if (productId == null) {
            return 0;
        }
        return availableStock.getOrDefault(productId, 0);
    }

    /**
     * Retrieves currently reserved stock for a product.
     *
     * @param productId product identifier
     * @return reserved count
     */
    public int getReservedStock(String productId) {
        if (productId == null) {
            return 0;
        }
        return reservedStock.getOrDefault(productId, 0);
    }

    /**
     * Checks if a requested quantity is currently available in stock.
     *
     * @param productId product identifier
     * @param requestedQuantity quantity needed (> 0)
     * @return true if available stock >= requestedQuantity, false otherwise
     */
    public boolean isInStock(String productId, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            return false;
        }
        return getStock(productId) >= requestedQuantity;
    }

    /**
     * Reserves stock for an in-progress order or checkout.
     * Moves quantity from available to reserved.
     *
     * @param productId product identifier
     * @param quantity quantity to reserve (> 0)
     * @throws ValidationException if quantity <= 0
     * @throws InsufficientStockException if available stock is less than quantity
     */
    public synchronized void reserveStock(String productId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Quantity to reserve must be greater than 0");
        }
        int current = getStock(productId);
        if (current < quantity) {
            throw new InsufficientStockException(
                    "Cannot reserve " + quantity + " units of " + productId + "; only " + current + " available");
        }
        availableStock.put(productId, current - quantity);
        reservedStock.put(productId, getReservedStock(productId) + quantity);
    }

    /**
     * Releases previously reserved stock back to available stock (e.g. cart abandoned or order cancelled).
     *
     * @param productId product identifier
     * @param quantity quantity to release (> 0)
     * @throws ValidationException if quantity <= 0 or exceeds reserved count
     */
    public synchronized void releaseStock(String productId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Quantity to release must be greater than 0");
        }
        int reserved = getReservedStock(productId);
        if (quantity > reserved) {
            throw new ValidationException("Cannot release " + quantity + " units; only " + reserved + " reserved");
        }
        reservedStock.put(productId, reserved - quantity);
        availableStock.put(productId, getStock(productId) + quantity);
    }

    /**
     * Permanently deducts stock (completes checkout from reserved stock).
     *
     * @param productId product identifier
     * @param quantity units to deduct from reserved (> 0)
     * @throws ValidationException if quantity <= 0 or exceeds reserved count
     */
    public synchronized void deductStock(String productId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Quantity to deduct must be greater than 0");
        }
        int reserved = getReservedStock(productId);
        if (quantity > reserved) {
            throw new ValidationException("Cannot deduct " + quantity + " units; only " + reserved + " reserved");
        }
        reservedStock.put(productId, reserved - quantity);
    }

    /**
     * Replenishes stock by adding units to available stock.
     *
     * @param productId product identifier
     * @param quantity units to add (> 0)
     * @throws ValidationException if quantity <= 0
     */
    public synchronized void restock(String productId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Restock quantity must be greater than 0");
        }
        availableStock.put(productId, getStock(productId) + quantity);
        reservedStock.putIfAbsent(productId, 0);
    }
}
