package am.gortsup.testing.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a promotional discount coupon.
 */
public class Coupon {
    private final String code;
    private final DiscountType discountType;
    private final double discountValue;
    private final double minOrderAmount;
    private final LocalDate expiryDate;
    private final int usageLimit;
    private int timesUsed;

    public Coupon(String code, DiscountType discountType, double discountValue,
                  double minOrderAmount, LocalDate expiryDate, int usageLimit) {
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.expiryDate = expiryDate;
        this.usageLimit = usageLimit;
        this.timesUsed = 0;
    }

    public String getCode() {
        return code;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public double getMinOrderAmount() {
        return minOrderAmount;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public int getTimesUsed() {
        return timesUsed;
    }

    public void incrementTimesUsed() {
        this.timesUsed++;
    }

    public boolean isExpired(LocalDate currentDate) {
        return currentDate != null && currentDate.isAfter(expiryDate);
    }

    public boolean isUsageLimitReached() {
        return timesUsed >= usageLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coupon coupon = (Coupon) o;
        return Objects.equals(code, coupon.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
