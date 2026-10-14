package am.gortsup.testing.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Text processing and string manipulation utilities for formatting, masking, and analysis.
 */
public final class StringUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private StringUtils() {
        // Utility class
    }

    /**
     * Converts a string into a URL-friendly slug.
     * Rules:
     * - Trims and converts to lowercase
     * - Strips accents/diacritics
     * - Replaces spaces and underscores with single hyphen '-'
     * - Strips non-alphanumeric characters (except hyphen)
     * - Collapses consecutive hyphens into one and trims leading/trailing hyphens
     *
     * @param text the input string
     * @return URL slug, or empty string if input is null/blank
     */
    public static String toSlug(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String nowhitespace = WHITESPACE.matcher(text.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = slug.replaceAll("-{2,}", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Capitalizes the first letter of each word in a string, with the rest lowercase.
     * Example: "jOHn dOE" -> "John Doe"
     *
     * @param input the input string
     * @return capitalized string, or empty string if null
     */
    public static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String[] words = input.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
            if (i < words.length - 1) {
                result.append(" ");
            }
        }
        return result.toString();
    }

    /**
     * Truncates a string to a maximum length and appends a suffix (e.g. "...").
     * If the string length is less than or equal to maxLength, it is returned as-is.
     * If truncation occurs, the returned string total length (including suffix) will not exceed maxLength.
     *
     * @param text the text to truncate
     * @param maxLength maximum allowed length (must be >= suffix length)
     * @param suffix string to append (e.g. "...")
     * @return truncated string
     * @throws IllegalArgumentException if maxLength is negative or less than suffix length
     */
    public static String truncate(String text, int maxLength, String suffix) {
        if (text == null) {
            return null;
        }
        if (suffix == null) {
            suffix = "";
        }
        if (maxLength < suffix.length()) {
            throw new IllegalArgumentException("maxLength cannot be less than suffix length");
        }
        if (text.length() <= maxLength) {
            return text;
        }
        int keep = maxLength - suffix.length();
        return text.substring(0, keep) + suffix;
    }

    /**
     * Counts occurrences of a substring within a text (case-sensitive).
     *
     * @param text the full text
     * @param sub the substring to count
     * @return number of occurrences (0 if text/sub is null or empty)
     */
    public static int countOccurrences(String text, String sub) {
        if (text == null || sub == null || sub.isEmpty() || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * Checks if a string is a palindrome, ignoring case, spaces, and punctuation.
     * Examples: "Racecar" -> true, "A man, a plan, a canal: Panama" -> true.
     *
     * @param text the string to evaluate
     * @return true if palindrome, false otherwise (null returns false)
     */
    public static boolean isPalindrome(String text) {
        if (text == null) {
            return false;
        }
        String cleaned = text.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        if (cleaned.isEmpty()) {
            return false;
        }
        int left = 0;
        int right = cleaned.length() - 1;
        while (left < right) {
            if (cleaned.charAt(left) != cleaned.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    /**
     * Masks a credit card or account number, keeping only the last 4 digits visible.
     * Strips non-digits first. If digits count is less than 4, masks all digits.
     * Example: "4111 2222 3333 4444" -> "************4444"
     *
     * @param cardNumber the card number string
     * @return masked string
     */
    public static String maskCreditCard(String cardNumber) {
        if (cardNumber == null) {
            return "";
        }
        String digits = cardNumber.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            return "*".repeat(digits.length());
        }
        int maskLength = digits.length() - 4;
        return "*".repeat(maskLength) + digits.substring(maskLength);
    }

    /**
     * Reverses the words in a sentence preserving single spaces between words.
     * Example: "The quick brown fox" -> "fox brown quick The"
     *
     * @param sentence the input sentence
     * @return reversed words sentence, or empty string if null/blank
     */
    public static String reverseWords(String sentence) {
        if (sentence == null || sentence.trim().isEmpty()) {
            return "";
        }
        String[] words = sentence.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(words[i]);
            if (i > 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
