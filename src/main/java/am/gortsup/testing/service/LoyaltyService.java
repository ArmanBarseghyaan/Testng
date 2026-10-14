package am.gortsup.testing.service;

import am.gortsup.testing.exception.ValidationException;
import am.gortsup.testing.model.LoyaltyTier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing customer loyalty rewards, points accumulation, tier levels, and point transfers.
 */
public class LoyaltyService {

    private final Map<String, Integer> currentPoints = new ConcurrentHashMap<>();
    private final Map<String, Integer> lifetimePoints = new ConcurrentHashMap<>();

    /**
     * Awards loyalty reward points to a customer.
     * Updates both current spendable balance and lifetime points.
     *
     * @param userId user identifier
     * @param pointsEarned positive points to add (> 0)
     * @throws ValidationException if userId is empty or pointsEarned <= 0
     */
    public synchronized void addPoints(String userId, int pointsEarned) {
        validateUserId(userId);
        if (pointsEarned <= 0) {
            throw new ValidationException("Points earned must be strictly positive: " + pointsEarned);
        }
        currentPoints.put(userId, getPoints(userId) + pointsEarned);
        lifetimePoints.put(userId, getLifetimePoints(userId) + pointsEarned);
    }

    /**
     * Spends/redeems points from the customer's available balance.
     * Lifetime points are not decreased by redemption.
     *
     * @param userId user identifier
     * @param pointsToRedeem points to spend (> 0)
     * @throws ValidationException if pointsToRedeem <= 0 or exceeds available balance
     */
    public synchronized void redeemPoints(String userId, int pointsToRedeem) {
        validateUserId(userId);
        if (pointsToRedeem <= 0) {
            throw new ValidationException("Points to redeem must be strictly positive: " + pointsToRedeem);
        }
        int available = getPoints(userId);
        if (pointsToRedeem > available) {
            throw new ValidationException("Insufficient points balance. Available: " + available + ", requested: " + pointsToRedeem);
        }
        currentPoints.put(userId, available - pointsToRedeem);
    }

    /**
     * Returns current spendable points for a customer.
     *
     * @param userId user identifier
     * @return points balance (0 if none)
     */
    public int getPoints(String userId) {
        if (userId == null) {
            return 0;
        }
        return currentPoints.getOrDefault(userId, 0);
    }

    /**
     * Returns total accumulated lifetime points for tier qualification.
     *
     * @param userId user identifier
     * @return lifetime points (0 if none)
     */
    public int getLifetimePoints(String userId) {
        if (userId == null) {
            return 0;
        }
        return lifetimePoints.getOrDefault(userId, 0);
    }

    /**
     * Determines loyalty tier based on cumulative lifetime points.
     * Tiers:
     * - 0 to 999: BRONZE
     * - 1000 to 4999: SILVER
     * - 5000 to 9999: GOLD
     * - 10000 and above: PLATINUM
     *
     * @param totalLifetimePoints lifetime points (must be >= 0)
     * @return LoyaltyTier
     * @throws ValidationException if points < 0
     */
    public LoyaltyTier calculateTier(int totalLifetimePoints) {
        if (totalLifetimePoints < 0) {
            throw new ValidationException("Lifetime points cannot be negative: " + totalLifetimePoints);
        }
        if (totalLifetimePoints >= 10000) {
            return LoyaltyTier.PLATINUM;
        } else if (totalLifetimePoints >= 5000) {
            return LoyaltyTier.GOLD;
        } else if (totalLifetimePoints >= 1000) {
            return LoyaltyTier.SILVER;
        } else {
            return LoyaltyTier.BRONZE;
        }
    }

    /**
     * Retrieves the current loyalty tier for a user.
     *
     * @param userId user identifier
     * @return current tier
     */
    public LoyaltyTier getUserTier(String userId) {
        return calculateTier(getLifetimePoints(userId));
    }

    /**
     * Returns exclusive percentage discount associated with a loyalty tier:
     * - BRONZE: 0.0%
     * - SILVER: 5.0%
     * - GOLD: 10.0%
     * - PLATINUM: 15.0%
     *
     * @param tier loyalty tier
     * @return discount percentage
     */
    public double getTierDiscountPercentage(LoyaltyTier tier) {
        if (tier == null) {
            return 0.0;
        }
        switch (tier) {
            case SILVER:
                return 5.0;
            case GOLD:
                return 10.0;
            case PLATINUM:
                return 15.0;
            case BRONZE:
            default:
                return 0.0;
        }
    }

    /**
     * Transfers points from one user to another.
     *
     * @param fromUserId source user ID
     * @param toUserId recipient user ID
     * @param points points to transfer (> 0)
     * @throws ValidationException if arguments are invalid, users are identical, or insufficient balance
     */
    public synchronized void transferPoints(String fromUserId, String toUserId, int points) {
        validateUserId(fromUserId);
        validateUserId(toUserId);
        if (fromUserId.equals(toUserId)) {
            throw new ValidationException("Cannot transfer points to the same account");
        }
        if (points <= 0) {
            throw new ValidationException("Transfer points must be strictly positive");
        }
        int senderBalance = getPoints(fromUserId);
        if (senderBalance < points) {
            throw new ValidationException("Sender has insufficient points: " + senderBalance);
        }

        currentPoints.put(fromUserId, senderBalance - points);
        currentPoints.put(toUserId, getPoints(toUserId) + points);
        lifetimePoints.put(toUserId, getLifetimePoints(toUserId) + points);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new ValidationException("User ID cannot be null or empty");
        }
    }
}
