package am.gortsup.testing.service;

import am.gortsup.testing.exception.InvalidCouponException;
import am.gortsup.testing.exception.ResourceNotFoundException;
import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.CartItem;
import am.gortsup.testing.model.Coupon;
import am.gortsup.testing.model.DiscountType;
import am.gortsup.testing.model.Product;
import am.gortsup.testing.util.MathAndFinanceUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing user shopping carts, item management, coupons, and cart totals calculation.
 */
public class ShoppingCartService {

    private final Map<String, Map<String, CartItem>> carts = new ConcurrentHashMap<>();
    private final Map<String, Coupon> appliedCoupons = new ConcurrentHashMap<>();

    private static final int MAX_ITEM_QUANTITY = 99;

    /**
     * Adds an item to the shopping cart. If already present, increments quantity.
     *
     * @param cartId shopping cart identifier
     * @param product product to add
     * @param quantity quantity to add (1 to 99)
     * @throws ValidationException if product is null, inactive, or quantity is out of range
     */
    public void addItem(String cartId, Product product, int quantity) {
        validateCartId(cartId);
        if (product == null) {
            throw new ValidationException("Product cannot be null");
        }
        if (!product.isActive()) {
            throw new ValidationException("Cannot add inactive product to cart: " + product.getId());
        }
        if (quantity <= 0 || quantity > MAX_ITEM_QUANTITY) {
            throw new ValidationException("Quantity must be between 1 and " + MAX_ITEM_QUANTITY);
        }

        Map<String, CartItem> cart = carts.computeIfAbsent(cartId, k -> new LinkedHashMap<>());
        CartItem existing = cart.get(product.getId());
        if (existing != null) {
            int newQty = existing.getQuantity() + quantity;
            if (newQty > MAX_ITEM_QUANTITY) {
                throw new ValidationException("Cart quantity for product exceeds maximum limit of " + MAX_ITEM_QUANTITY);
            }
            existing.setQuantity(newQty);
        } else {
            cart.put(product.getId(), new CartItem(product, quantity));
        }
    }

    /**
     * Removes a product completely from the cart.
     *
     * @param cartId cart ID
     * @param productId product ID
     * @throws ResourceNotFoundException if item not in cart
     */
    public void removeItem(String cartId, String productId) {
        validateCartId(cartId);
        Map<String, CartItem> cart = carts.get(cartId);
        if (cart == null || !cart.containsKey(productId)) {
            throw new ResourceNotFoundException("Product " + productId + " is not in cart " + cartId);
        }
        cart.remove(productId);
    }

    /**
     * Updates the exact quantity of a product in the cart.
     *
     * @param cartId cart ID
     * @param productId product ID
     * @param newQuantity new quantity (1 to 99)
     * @throws ResourceNotFoundException if product not in cart
     * @throws ValidationException if quantity is not between 1 and 99
     */
    public void updateQuantity(String cartId, String productId, int newQuantity) {
        validateCartId(cartId);
        if (newQuantity <= 0 || newQuantity > MAX_ITEM_QUANTITY) {
            throw new ValidationException("Quantity must be between 1 and " + MAX_ITEM_QUANTITY);
        }
        Map<String, CartItem> cart = carts.get(cartId);
        if (cart == null || !cart.containsKey(productId)) {
            throw new ResourceNotFoundException("Product " + productId + " is not in cart " + cartId);
        }
        cart.get(productId).setQuantity(newQuantity);
    }

    /**
     * Applies a discount coupon to the cart.
     * Validates expiration date, usage limit, and minimum order amount.
     *
     * @param cartId cart ID
     * @param coupon coupon entity
     * @param currentDate date against which to check expiration
     * @throws ValidationException if coupon is null
     * @throws InvalidCouponException if coupon is expired, depleted, or cart subtotal < minOrderAmount
     */
    public void applyCoupon(String cartId, Coupon coupon, LocalDate currentDate) {
        validateCartId(cartId);
        if (coupon == null) {
            throw new ValidationException("Coupon cannot be null");
        }
        if (coupon.isExpired(currentDate)) {
            throw new InvalidCouponException("Coupon " + coupon.getCode() + " is expired");
        }
        if (coupon.isUsageLimitReached()) {
            throw new InvalidCouponException("Coupon " + coupon.getCode() + " usage limit reached");
        }

        double subtotal = calculateSubtotal(cartId);
        if (subtotal < coupon.getMinOrderAmount()) {
            throw new InvalidCouponException("Minimum order amount of $" + coupon.getMinOrderAmount() +
                    " not met (current subtotal: $" + subtotal + ")");
        }

        appliedCoupons.put(cartId, coupon);
    }

    /**
     * Removes the currently applied coupon from the cart.
     *
     * @param cartId cart ID
     */
    public void removeCoupon(String cartId) {
        validateCartId(cartId);
        appliedCoupons.remove(cartId);
    }

    /**
     * Calculates the cart subtotal before discounts and taxes.
     *
     * @param cartId cart ID
     * @return subtotal rounded to 2 decimal places
     */
    public double calculateSubtotal(String cartId) {
        validateCartId(cartId);
        Map<String, CartItem> cart = carts.get(cartId);
        if (cart == null || cart.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (CartItem item : cart.values()) {
            sum += item.getSubtotal();
        }
        return MathAndFinanceUtils.roundToDecimalPlaces(sum, 2);
    }

    /**
     * Calculates discount amount based on the currently applied coupon.
     *
     * @param cartId cart ID
     * @return discount monetary amount (0.0 if no coupon applied)
     */
    public double calculateDiscountAmount(String cartId) {
        validateCartId(cartId);
        Coupon coupon = appliedCoupons.get(cartId);
        if (coupon == null) {
            return 0.0;
        }

        double subtotal = calculateSubtotal(cartId);
        if (subtotal < coupon.getMinOrderAmount()) {
            return 0.0;
        }

        double discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal * (coupon.getDiscountValue() / 100.0);
        } else {
            discount = Math.min(subtotal, coupon.getDiscountValue());
        }
        return MathAndFinanceUtils.roundToDecimalPlaces(discount, 2);
    }

    /**
     * Calculates final total: (subtotal - discount) + tax.
     *
     * @param cartId cart ID
     * @param taxRatePercentage tax rate (0.0 to 100.0)
     * @return total rounded to 2 decimal places
     * @throws ValidationException if tax rate is invalid
     */
    public double calculateTotal(String cartId, double taxRatePercentage) {
        validateCartId(cartId);
        if (taxRatePercentage < 0.0 || taxRatePercentage > 100.0) {
            throw new ValidationException("Tax rate must be between 0.0 and 100.0");
        }

        double subtotal = calculateSubtotal(cartId);
        double discount = calculateDiscountAmount(cartId);
        double taxableAmount = Math.max(0.0, subtotal - discount);
        double tax = MathAndFinanceUtils.calculateTax(taxableAmount, taxRatePercentage);
        return MathAndFinanceUtils.roundToDecimalPlaces(taxableAmount + tax, 2);
    }

    /**
     * Clears all items and applied coupon from the cart.
     *
     * @param cartId cart ID
     */
    public void clearCart(String cartId) {
        validateCartId(cartId);
        Map<String, CartItem> cart = carts.get(cartId);
        if (cart != null) {
            cart.clear();
        }
        appliedCoupons.remove(cartId);
    }

    /**
     * Retrieves all items in the cart.
     *
     * @param cartId cart ID
     * @return unmodifiable list of cart items
     */
    public List<CartItem> getCartItems(String cartId) {
        validateCartId(cartId);
        Map<String, CartItem> cart = carts.get(cartId);
        if (cart == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(cart.values()));
    }

    /**
     * Returns total number of individual items (sum of quantities) in the cart.
     *
     * @param cartId cart ID
     * @return sum of quantities
     */
    public int getItemCount(String cartId) {
        validateCartId(cartId);
        Map<String, CartItem> cart = carts.get(cartId);
        if (cart == null) {
            return 0;
        }
        int total = 0;
        for (CartItem item : cart.values()) {
            total += item.getQuantity();
        }
        return total;
    }

    /**
     * Gets currently applied coupon, if any.
     *
     * @param cartId cart ID
     * @return optional coupon
     */
    public Optional<Coupon> getAppliedCoupon(String cartId) {
        validateCartId(cartId);
        return Optional.ofNullable(appliedCoupons.get(cartId));
    }

    private void validateCartId(String cartId) {
        if (cartId == null || cartId.trim().isEmpty()) {
            throw new ValidationException("Cart ID cannot be null or empty");
        }
    }
}
