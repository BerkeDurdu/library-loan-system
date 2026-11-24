package com.svv.library.model;

import com.svv.library.util.IsbnValidator;

/**
 * A book title in the catalogue with a number of physical copies (FR-02).
 */
public class Book {

    public static final int MIN_COPIES = 1;
    public static final int MAX_COPIES = 10;

    private final String isbn;
    private final String title;
    private final int totalCopies;
    private int availableCopies;

    public Book(String isbn, String title, int totalCopies) {
        if (!IsbnValidator.isValid(isbn)) {
            throw new IllegalArgumentException("Invalid ISBN-13: " + isbn);
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (totalCopies < MIN_COPIES || totalCopies > MAX_COPIES) {
            throw new IllegalArgumentException("Number of copies must be between 1 and 10");
        }
        this.isbn = IsbnValidator.normalize(isbn);
        this.title = title.trim();
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public boolean hasAvailableCopy() {
        return availableCopies > 0;
    }

    void checkOut() {
        if (!hasAvailableCopy()) {
            throw new IllegalStateException("No copy available for " + isbn);
        }
        availableCopies--;
    }

    void checkIn() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("All copies are already in the library for " + isbn);
        }
        availableCopies++;
    }

    @Override
    public String toString() {
        return title + " [" + isbn + "] " + availableCopies + "/" + totalCopies;
    }
}
