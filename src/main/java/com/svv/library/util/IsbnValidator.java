package com.svv.library.util;

/**
 * Validates ISBN-13 numbers (FR-02).
 * Rules: after removing hyphens and spaces there must be exactly 13 digits, the prefix must be
 * 978 or 979, and the last digit must be the correct check digit (weights 1,3,1,3,...).
 */
public final class IsbnValidator {

    private IsbnValidator() {
    }

    /** Removes hyphens and spaces, e.g. "978-0-306-40615-7" becomes "9780306406157". */
    public static String normalize(String isbn) {
        return isbn.replace("-", "").replace(" ", "");
    }

    public static boolean isValid(String isbn) {
        if (isbn == null) {
            return false;
        }
        String digits = normalize(isbn);
        if (digits.length() != 13 || !digits.chars().allMatch(Character::isDigit)) {
            return false;
        }
        if (!digits.startsWith("978") && !digits.startsWith("979")) {
            return false;
        }
        return computeCheckDigit(digits.substring(0, 12)) == digits.charAt(12) - '0';
    }

    /** Check digit = (10 - (weighted sum mod 10)) mod 10. */
    static int computeCheckDigit(String first12Digits) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = first12Digits.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (10 - (sum % 10)) % 10;
    }
}
