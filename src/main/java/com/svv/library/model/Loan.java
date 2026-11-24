package com.svv.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One borrowing of one book copy by one member (FR-03 .. FR-07).
 */
public class Loan {

    private final Member member;
    private final Book book;
    private final LocalDate borrowDate;
    private LocalDate dueDate;
    private int renewCount;
    private LocalDate returnDate;
    private BigDecimal fine = Money.ZERO;

    private Loan(Member member, Book book, LocalDate borrowDate) {
        this.member = member;
        this.book = book;
        this.borrowDate = borrowDate;
        this.dueDate = borrowDate.plusDays(member.getType().getLoanPeriodDays());
        this.renewCount = 0;
    }

    /** Creates a loan and takes one copy of the book off the shelf. Rules are checked in LibraryService. */
    public static Loan open(Member member, Book book, LocalDate borrowDate) {
        book.checkOut();
        return new Loan(member, book, borrowDate);
    }

    /** Lets the service reduce a member's balance (fine payment). */
    public static void applyPayment(Member member, BigDecimal amount) {
        member.reduceFine(amount);
    }

    public Member getMember() {
        return member;
    }

    public Book getBook() {
        return book;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public int getRenewCount() {
        return renewCount;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public BigDecimal getFine() {
        return fine;
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    /** Extends the due date by one more loan period. Rules are checked in LibraryService. */
    public void extend() {
        dueDate = dueDate.plusDays(member.getType().getLoanPeriodDays());
        renewCount++;
    }

    /** Marks the loan as returned, puts the copy back and charges the fine to the member. */
    public void close(LocalDate date, BigDecimal fineAmount) {
        this.returnDate = date;
        this.fine = Money.of(fineAmount);
        book.checkIn();
        member.addFine(this.fine);
    }
}
