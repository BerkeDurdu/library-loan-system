package com.svv.library.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for FR-02 (adding a book): ISBN, title and number of copies (valid range 1..10).
 */
class BookTest {

    private static final String ISBN = "9780306406157";

    // Normal book: ISBN written with hyphens must be stored without them,
    // title is trimmed and all copies are on the shelf at the start.
    @Test
    @DisplayName("TC-BOOK-01 valid book is created and ISBN is normalised")
    void validBook() {
        Book b = new Book("978-0-306-40615-7", "  Clean Code ", 3);
        assertAll(
                () -> assertEquals(ISBN, b.getIsbn()),
                () -> assertEquals("Clean Code", b.getTitle()),
                () -> assertEquals(3, b.getTotalCopies()),
                () -> assertEquals(3, b.getAvailableCopies()),
                () -> assertTrue(b.hasAvailableCopy()));
    }

    // BVA on copies: 1 (lower boundary) and 10 (upper boundary) are both valid.
    @ParameterizedTest(name = "TC-BOOK-02 BVA: {0} copies is accepted")
    @ValueSource(ints = {1, 10})
    void copiesOnBoundariesAccepted(int copies) {
        assertEquals(copies, new Book(ISBN, "Clean Code", copies).getTotalCopies());
    }

    // BVA on copies: 0 (just below) and 11 (just above) are invalid. -5 is a far invalid value.
    @ParameterizedTest(name = "TC-BOOK-03 BVA: {0} copies is rejected")
    @ValueSource(ints = {0, 11, -5})
    void copiesOutsideRangeRejected(int copies) {
        assertThrows(IllegalArgumentException.class, () -> new Book(ISBN, "Clean Code", copies));
    }

    // Negative: a wrong ISBN must stop the book from being created.
    @Test
    @DisplayName("TC-BOOK-04 invalid ISBN is rejected")
    void invalidIsbn() {
        assertThrows(IllegalArgumentException.class, () -> new Book("9780306406158", "Clean Code", 1));
    }

    // Negative: null, "" and blank titles are not allowed.
    @ParameterizedTest(name = "TC-BOOK-05 null / empty title is rejected")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankTitle(String title) {
        assertThrows(IllegalArgumentException.class, () -> new Book(ISBN, title, 1));
    }

    // When the only copy is lent out, hasAvailableCopy() must become false,
    // and lending again must fail (state-based test of checkOut / checkIn).
    @Test
    @DisplayName("TC-BOOK-06 copy counter goes down on checkout and up on checkin")
    void checkoutAndCheckin() {
        Book b = new Book(ISBN, "Clean Code", 1);
        b.checkOut();
        assertFalse(b.hasAvailableCopy());
        assertThrows(IllegalStateException.class, b::checkOut);
        b.checkIn();
        assertEquals(1, b.getAvailableCopies());
        // returning more copies than the library owns is not possible
        assertThrows(IllegalStateException.class, b::checkIn);
    }
}
