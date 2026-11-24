package com.svv.library.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for FR-02 (ISBN-13 validation).
 *
 * Input domain of an ISBN string was split into classes (Equivalence Partitioning):
 *   valid:   13 digits + 978/979 prefix + correct check digit (with or without hyphens)
 *   invalid: wrong check digit, wrong length, non-digit characters, wrong prefix, null/empty
 * I pick one representative from each class, plus the length boundaries 12 / 13 / 14 (BVA).
 */
class IsbnValidatorTest {

    // ---------- valid classes (EP) ----------

    // Representative of the "plain 13 digits, prefix 978" class. Should be accepted.
    @Test
    @DisplayName("TC-ISBN-01 valid ISBN with 978 prefix is accepted")
    void validIsbn978() {
        assertTrue(IsbnValidator.isValid("9780306406157"));
    }

    // Same number but written the way it is printed on books (with hyphens).
    // The validator must ignore the hyphens.
    @Test
    @DisplayName("TC-ISBN-02 valid ISBN written with hyphens is accepted")
    void validIsbnWithHyphens() {
        assertTrue(IsbnValidator.isValid("978-0-306-40615-7"));
    }

    // The second allowed prefix (979) is a separate valid class, so it gets its own test.
    @Test
    @DisplayName("TC-ISBN-03 valid ISBN with 979 prefix is accepted")
    void validIsbn979() {
        assertTrue(IsbnValidator.isValid("979-10-90636-07-1"));
    }

    // ---------- invalid classes (EP + negative testing) ----------

    // Each value below belongs to a different invalid class:
    //  - correct length and prefix but the last (check) digit is wrong
    //  - a letter inside the number
    //  - prefix 977 with a *correct* checksum, so ONLY the prefix rule can reject it
    //  - only hyphens / only spaces (nothing left after normalising)
    @ParameterizedTest(name = "TC-ISBN-04 invalid ISBN \"{0}\" is rejected")
    @ValueSource(strings = {"9780306406158", "97803064061X7", "9770306406158", "---", "   "})
    void invalidIsbnClasses(String isbn) {
        assertFalse(IsbnValidator.isValid(isbn));
    }

    // null and "" are the classic negative inputs; the method must return false, not crash.
    @ParameterizedTest(name = "TC-ISBN-05 null / empty input -> false")
    @NullAndEmptySource
    void nullAndEmptyAreRejected(String isbn) {
        assertFalse(IsbnValidator.isValid(isbn));
    }

    // ---------- Boundary Value Analysis on the length (boundary = 13) ----------

    // 12 digits = just below the boundary -> invalid
    @Test
    @DisplayName("TC-ISBN-06 BVA: 12 digits (13-1) is rejected")
    void twelveDigitsRejected() {
        assertFalse(IsbnValidator.isValid("978030640615"));
    }

    // 13 digits = on the boundary -> valid (the same number as TC-ISBN-01, another book)
    @Test
    @DisplayName("TC-ISBN-07 BVA: exactly 13 digits is accepted")
    void thirteenDigitsAccepted() {
        assertTrue(IsbnValidator.isValid("9783161484100"));
    }

    // 14 digits = just above the boundary -> invalid
    @Test
    @DisplayName("TC-ISBN-08 BVA: 14 digits (13+1) is rejected")
    void fourteenDigitsRejected() {
        assertFalse(IsbnValidator.isValid("97803064061570"));
    }

    // ---------- helper behaviour ----------

    // normalize() is used before storing a book, so "978-0-..." and "9780..." are the same key.
    @Test
    @DisplayName("TC-ISBN-09 normalize removes hyphens and spaces")
    void normalizeRemovesSeparators() {
        assertEquals("9780306406157", IsbnValidator.normalize("978-0 306-40615 7"));
    }

    // Check digit formula checked directly against a known published ISBN (…723 -> 4).
    @Test
    @DisplayName("TC-ISBN-10 check digit is calculated correctly")
    void checkDigitFormula() {
        assertEquals(4, IsbnValidator.computeCheckDigit("978014300723"));
    }
}
