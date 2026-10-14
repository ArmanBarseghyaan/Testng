package am.gortsup.testing.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mathematical and financial calculation utilities for commerce, taxation, interest, and payments.
 */
public final class MathAndFinanceUtils {

    private MathAndFinanceUtils() {
        // Utility class
    }

    /**
     * Calculates tax amount on a given subtotal.
     * Formula: subtotal * (taxRatePercentage / 100.0) rounded to 2 decimal places.
     *
     * @param amount the base monetary amount (must be non-negative)
     * @param taxRatePercentage the tax rate percentage (0.0 to 100.0)
     * @return tax amount rounded to 2 decimal places
     * @throws IllegalArgumentException if amount is negative or tax rate is outside [0.0, 100.0]
     */
    public static double calculateTax(double amount, double taxRatePercentage) {
        if (amount < 0.0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (taxRatePercentage < 0.0 || taxRatePercentage > 100.0) {
            throw new IllegalArgumentException("Tax rate must be between 0.0 and 100.0");
        }
        double tax = amount * (taxRatePercentage / 100.0);
        return roundToDecimalPlaces(tax, 2);
    }

    /**
     * Calculates discounted amount given a percentage discount.
     * Formula: amount * (1.0 - discountPercentage / 100.0) rounded to 2 decimal places.
     *
     * @param amount the original price (must be non-negative)
     * @param discountPercentage discount percentage (0.0 to 100.0)
     * @return discounted final price rounded to 2 decimal places
     * @throws IllegalArgumentException if amount is negative or discount percentage outside [0.0, 100.0]
     */
    public static double calculateDiscount(double amount, double discountPercentage) {
        if (amount < 0.0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (discountPercentage < 0.0 || discountPercentage > 100.0) {
            throw new IllegalArgumentException("Discount percentage must be between 0.0 and 100.0");
        }
        double discounted = amount * (1.0 - (discountPercentage / 100.0));
        return roundToDecimalPlaces(discounted, 2);
    }

    /**
     * Rounds a floating-point value to a specified number of decimal places using HALF_UP rounding.
     *
     * @param value the number to round
     * @param places decimal places (must be non-negative)
     * @return rounded value
     * @throws IllegalArgumentException if places is negative
     */
    public static double roundToDecimalPlaces(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException("Decimal places cannot be negative");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return value;
        }
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Validates credit card number checksum using the Luhn Algorithm (Mod 10).
     *
     * @param cardNumber the card number string (spaces and hyphens ignored)
     * @return true if valid according to Luhn algorithm, false otherwise
     */
    public static boolean isValidLuhn(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        String clean = cardNumber.replaceAll("[^0-9]", "");
        if (clean.length() < 8 || clean.length() > 19) {
            return false;
        }

        int sum = 0;
        boolean alternate = false;
        for (int i = clean.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(clean.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    /**
     * Converts a monetary amount from one currency to another using an exchange rate.
     *
     * @param amount the source amount (must be non-negative)
     * @param exchangeRate the conversion rate multiplier (must be strictly positive)
     * @return converted amount rounded to 2 decimal places
     * @throws IllegalArgumentException if amount is negative or exchangeRate <= 0
     */
    public static double convertCurrency(double amount, double exchangeRate) {
        if (amount < 0.0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (exchangeRate <= 0.0) {
            throw new IllegalArgumentException("Exchange rate must be strictly positive");
        }
        return roundToDecimalPlaces(amount * exchangeRate, 2);
    }

    /**
     * Calculates compound interest: A = P * (1 + r/n)^(n*t)
     *
     * @param principal original principal investment (must be >= 0)
     * @param annualRate annual interest rate in percent (e.g., 5 for 5%)
     * @param timesCompoundedPerYear number of times interest is compounded per year (must be >= 1)
     * @param years duration in years (must be >= 0)
     * @return future total value rounded to 2 decimal places
     * @throws IllegalArgumentException if any argument violates constraints
     */
    public static double calculateCompoundInterest(double principal, double annualRate, int timesCompoundedPerYear, int years) {
        if (principal < 0.0) {
            throw new IllegalArgumentException("Principal cannot be negative");
        }
        if (annualRate < 0.0) {
            throw new IllegalArgumentException("Annual rate cannot be negative");
        }
        if (timesCompoundedPerYear < 1) {
            throw new IllegalArgumentException("Compounding frequency must be at least 1");
        }
        if (years < 0) {
            throw new IllegalArgumentException("Years cannot be negative");
        }

        double rateDecimal = annualRate / 100.0;
        double base = 1.0 + (rateDecimal / timesCompoundedPerYear);
        double exponent = (double) timesCompoundedPerYear * years;
        double amount = principal * Math.pow(base, exponent);
        return roundToDecimalPlaces(amount, 2);
    }

    /**
     * Calculates monthly installment (fixed loan payment) based on standard amortization formula:
     * M = P * [ r(1+r)^n ] / [ (1+r)^n - 1 ]
     * where r is monthly interest rate (annualRate / 100 / 12)
     * If annualRate is 0, M = P / n.
     *
     * @param principal loan principal amount (> 0)
     * @param annualInterestRate annual interest rate percentage (>= 0)
     * @param months loan term in months (>= 1)
     * @return monthly payment amount rounded to 2 decimal places
     * @throws IllegalArgumentException if principal <= 0, annualInterestRate < 0, or months < 1
     */
    public static double calculateMonthlyInstallment(double principal, double annualInterestRate, int months) {
        if (principal <= 0.0) {
            throw new IllegalArgumentException("Principal must be strictly positive");
        }
        if (annualInterestRate < 0.0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        if (months < 1) {
            throw new IllegalArgumentException("Loan term must be at least 1 month");
        }

        if (annualInterestRate == 0.0) {
            return roundToDecimalPlaces(principal / months, 2);
        }

        double monthlyRate = (annualInterestRate / 100.0) / 12.0;
        double factor = Math.pow(1.0 + monthlyRate, months);
        double payment = principal * (monthlyRate * factor) / (factor - 1.0);
        return roundToDecimalPlaces(payment, 2);
    }

    /**
     * Calculates percentage change from oldValue to newValue.
     * Formula: ((newValue - oldValue) / oldValue) * 100.0
     *
     * @param oldValue baseline value (must not be zero)
     * @param newValue updated value
     * @return percentage change rounded to 2 decimal places
     * @throws IllegalArgumentException if oldValue is zero
     */
    public static double calculatePercentageChange(double oldValue, double newValue) {
        if (oldValue == 0.0) {
            throw new IllegalArgumentException("Old value cannot be zero when calculating percentage change");
        }
        double change = ((newValue - oldValue) / Math.abs(oldValue)) * 100.0;
        return roundToDecimalPlaces(change, 2);
    }
}
