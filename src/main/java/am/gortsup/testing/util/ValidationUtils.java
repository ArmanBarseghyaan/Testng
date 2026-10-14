package am.gortsup.testing.util;

import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * Utility functions for validating user inputs, formats, identifiers, and sanitizing strings.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9]+([._%+-][A-Za-z0-9]+)*@[A-Za-z0-9]+([.-][A-Za-z0-9]+)*\\.[A-Za-z]{2,63}$"
    );

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{2,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)$"
    );

    private ValidationUtils() {
        // Utility class
    }

    /**
     * Validates an email address.
     * Rules:
     * - Must not be null or blank
     * - Must contain username, '@', and valid domain with TLD of at least 2 characters
     * - Must not have consecutive dots or start/end with dot in username or domain
     *
     * @param email the email to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String trimmed = email.trim();
        if (trimmed.length() > 254) {
            return false;
        }
        return EMAIL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Validates password strength according to standard security rules.
     * Rules:
     * - Minimum 8 characters, maximum 64 characters
     * - At least one uppercase letter (A-Z)
     * - At least one lowercase letter (a-z)
     * - At least one numeric digit (0-9)
     * - At least one special symbol from: !@#$%^&*()-_+=[]{}|;:',.<>?/
     * - Must not contain any whitespace characters
     *
     * @param password the password to check
     * @return true if password meets all complexity rules, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        if (password.length() < 8 || password.length() > 64) {
            return false;
        }
        if (password.contains(" ") || password.contains("\t") || password.contains("\n")) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        String specialChars = "!@#$%^&*()-_+=[]{}|;:',.<>?/";

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (specialChars.indexOf(c) >= 0) hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Validates a phone number.
     * Rules:
     * - Must not be null or blank
     * - Stripped digits count must be between 7 and 15 (E.164 standard)
     * - May include an optional leading '+' and separators (dashes, spaces, dots, parentheses)
     *
     * @param phone the phone number string
     * @return true if valid, false otherwise
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        String trimmed = phone.trim();
        if (!trimmed.matches("^(\\+?[0-9]{1,4}[\\s.-]?)?(\\([0-9]{1,4}\\)|[0-9]{1,4})[\\s.-]?[0-9]{1,5}[\\s.-]?[0-9]{1,5}([\\s.-]?[0-9]{1,5})?$")) {
            return false;
        }
        String digitsOnly = trimmed.replaceAll("[^0-9]", "");
        return digitsOnly.length() >= 7 && digitsOnly.length() <= 15;
    }

    /**
     * Validates postal code format by country code.
     * Supported countries:
     * - "US": 5 digits or 5 digits followed by hyphen and 4 digits (e.g. "90210" or "90210-1234")
     * - "UK" / "GB": standard UK outward and inward code (e.g. "SW1A 1AA", "W1A 0AX")
     * - "AM": Armenia 4 numeric digits (e.g. "0010")
     * - "CA": Canada alphanumeric A1A 1A1 (e.g. "K1A 0B1")
     * - Any other country: 3 to 10 alphanumeric characters
     *
     * @param postalCode the postal code
     * @param countryCode 2-letter ISO country code (case-insensitive)
     * @return true if postal code matches country format, false otherwise
     */
    public static boolean isValidPostalCode(String postalCode, String countryCode) {
        if (postalCode == null || countryCode == null) {
            return false;
        }
        String code = postalCode.trim();
        String country = countryCode.trim().toUpperCase();

        switch (country) {
            case "US":
                return code.matches("^\\d{5}(-\\d{4})?$");
            case "UK":
            case "GB":
                return code.toUpperCase().matches("^[A-Z]{1,2}\\d[A-Z\\d]? ?\\d[A-Z]{2}$");
            case "AM":
                return code.matches("^\\d{4}$");
            case "CA":
                return code.toUpperCase().matches("^[A-Z]\\d[A-Z] ?\\d[A-Z]\\d$");
            default:
                return code.matches("^[A-Za-z0-9\\- ]{3,10}$");
        }
    }

    /**
     * Validates an International Bank Account Number (IBAN) using the ISO 7064 mod-97 check.
     *
     * @param iban the IBAN string
     * @return true if valid, false otherwise
     */
    public static boolean isValidIban(String iban) {
        if (iban == null) {
            return false;
        }
        String clean = iban.replaceAll("\\s+", "").toUpperCase();
        if (clean.length() < 15 || clean.length() > 34) {
            return false;
        }
        if (!clean.matches("^[A-Z]{2}[0-9]{2}[A-Z0-9]+$")) {
            return false;
        }

        // Move first 4 characters to the end
        String rearranged = clean.substring(4) + clean.substring(0, 4);
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isDigit(c)) {
                numeric.append(c);
            } else {
                numeric.append((c - 'A') + 10);
            }
        }

        BigInteger bigInt = new BigInteger(numeric.toString());
        return bigInt.mod(BigInteger.valueOf(97)).intValue() == 1;
    }

    /**
     * Validates an IPv4 address.
     * Must consist of 4 decimal octets (0-255) separated by dots, no leading zeros unless single '0'.
     *
     * @param ip the IP address string
     * @return true if valid IPv4, false otherwise
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        String trimmed = ip.trim();
        if (!IPV4_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        String[] parts = trimmed.split("\\.");
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (part.length() > 1 && part.startsWith("0")) return false; // leading zero
            int num = Integer.parseInt(part);
            if (num < 0 || num > 255) return false;
        }
        return true;
    }

    /**
     * Validates whether a given URL is a well-formed HTTP/HTTPS URL.
     *
     * @param url the URL string
     * @return true if valid URL, false otherwise
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return URL_PATTERN.matcher(url.trim()).matches();
    }

    /**
     * Sanitizes user input to prevent HTML/XSS injection.
     * Converts:
     * - '&' -> '&amp;'
     * - '<' -> '&lt;'
     * - '>' -> '&gt;'
     * - '"' -> '&quot;'
     * - '\'' -> '&#x27;'
     *
     * @param input the raw string
     * @return sanitized string, or empty string if input was null
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * Checks if a numeric value is strictly positive (> 0).
     *
     * @param number the number to check
     * @return true if not null and > 0, false otherwise
     */
    public static boolean isPositiveNumber(Number number) {
        if (number == null) {
            return false;
        }
        return number.doubleValue() > 0.0;
    }
}
