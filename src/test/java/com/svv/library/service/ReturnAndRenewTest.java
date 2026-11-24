package com.svv.library.service;

import com.svv.library.model.Book;
import com.svv.library.model.Loan;
import com.svv.library.model.Member;
import com.svv.library.model.MemberType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for FR-05 (returning a book) and FR-07 (renewing a loan).
 * Uses the REAL FineCalculator, so these are small integration tests between
 * LibraryService, Loan, Book, Member and FineCalculator.
 */
class ReturnAndRenewTest {

    private static final LocalDate BORROWED = LocalDate.of(2026, 11, 2);
    private static final LocalDate DUE = BORROWED.plusDays(14);   // student loan period
    private static final String ISBN = "9780306406157";
    private static final String ID = "M0001";

    private LibraryService library;
    private Loan loan;

    // Prefix values: every test starts with one student who has borrowed one book on 2 Nov 2026.
    @BeforeEach
    void setUp() {
        library = new LibraryService();
        library.registerMember(new Member(ID, "Ayse Yilmaz", MemberType.STUDENT));
        library.addBook(new Book(ISBN, "Software Testing", 1));
        loan = library.borrowBook(ID, ISBN, BORROWED);
    }

    // =====================================================================  FR-05 return

    // BVA around the due date: returning before or ON the due date is free,
    // the first day after the due date costs 1 TL. 10 days late = 7*1 + 3*2 = 13 TL (found D1).
    @ParameterizedTest(name = "TC-RET-01 returned {0} day(s) after due date -> fine {1} TL")
    @CsvSource({"-5, 0.00", "0, 0.00", "1, 1.00", "10, 13.00"})
    void fineDependsOnReturnDate(int daysAfterDue, String expectedFine) {
        BigDecimal fine = library.returnBook(loan, DUE.plusDays(daysAfterDue));
        assertAll(
                () -> assertEquals(new BigDecimal(expectedFine), fine),
                () -> assertEquals(new BigDecimal(expectedFine), loan.getFine()),
                // the fine must also be added to the member's balance (observability)
                () -> assertEquals(new BigDecimal(expectedFine), library.findMember(ID).getOutstandingFine()));
    }

    // After a return the copy is back on the shelf and the loan is no longer active.
    @Test
    @DisplayName("TC-RET-02 return puts the copy back and closes the loan")
    void returnUpdatesState() {
        library.returnBook(loan, DUE);
        assertAll(
                () -> assertTrue(loan.isReturned()),
                () -> assertEquals(DUE, loan.getReturnDate()),
                () -> assertEquals(1, library.findBook(ISBN).getAvailableCopies()),
                () -> assertEquals(0, library.getActiveLoans(ID).size()));
    }

    // Fines from two late loans must add up: 1 TL + 13 TL = 14 TL.
    @Test
    @DisplayName("TC-RET-03 fines from several loans are added together")
    void finesAccumulate() {
        library.returnBook(loan, DUE.plusDays(1));
        Loan second = library.borrowBook(ID, ISBN, DUE.plusDays(1));
        library.returnBook(second, second.getDueDate().plusDays(10));
        assertEquals(new BigDecimal("14.00"), library.findMember(ID).getOutstandingFine());
    }

    // Negative: the same loan cannot be returned twice (would put an extra copy on the shelf).
    @Test
    @DisplayName("TC-RET-04 returning the same loan twice is rejected")
    void doubleReturn() {
        library.returnBook(loan, DUE);
        assertThrows(IllegalStateException.class, () -> library.returnBook(loan, DUE));
    }

    // Negative: return date before borrow date is impossible. Borrow date itself is allowed (BVA).
    @Test
    @DisplayName("TC-RET-05 return date before the borrow date is rejected")
    void returnBeforeBorrow() {
        assertThrows(IllegalArgumentException.class, () -> library.returnBook(loan, BORROWED.minusDays(1)));
        assertEquals(new BigDecimal("0.00"), library.returnBook(loan, BORROWED));
    }

    @Test
    @DisplayName("TC-RET-06 null return date is rejected")
    void nullReturnDate() {
        assertThrows(IllegalArgumentException.class, () -> library.returnBook(loan, null));
    }

    // =====================================================================  FR-07 renew
    //
    // Renewal decision table (checked in this order):
    //  C1 loan not returned | C2 date <= due date | C3 renewCount < 2 | C4 fine < 10 TL -> renew
    //  C1 false -> IllegalStateException, C2 false -> LOAN_OVERDUE,
    //  C3 false -> RENEWAL_LIMIT_REACHED, C4 false -> UNPAID_FINES

    // Normal renewal before the due date: due date moves 14 days forward, counter becomes 1.
    @Test
    @DisplayName("TC-REN-01 renew before due date extends the loan by 14 days")
    void renewBeforeDue() {
        library.renewLoan(loan, DUE.minusDays(3));
        assertEquals(DUE.plusDays(14), loan.getDueDate());
        assertEquals(1, loan.getRenewCount());
    }

    // BVA: renewing ON the due date is still allowed (the book is not late yet).
    // This test found defect D5 (the code refused renewal on the due date itself).
    @Tag("regression")
    @Test
    @DisplayName("TC-REN-02 BVA: renew exactly on the due date is allowed (D5)")
    void renewOnDueDate() {
        library.renewLoan(loan, DUE);
        assertEquals(DUE.plusDays(14), loan.getDueDate());
    }

    // BVA: one day after the due date the loan is overdue -> refused.
    @Test
    @DisplayName("TC-REN-03 BVA: renew one day after the due date -> LOAN_OVERDUE")
    void renewAfterDue() {
        LoanRefusedException e = assertThrows(LoanRefusedException.class,
                () -> library.renewLoan(loan, DUE.plusDays(1)));
        assertEquals(RefusalReason.LOAN_OVERDUE, e.getReason());
    }

    // BVA on the renewal counter: 1st and 2nd renewals pass, the 3rd one is refused.
    @Test
    @DisplayName("TC-REN-04 BVA: 2 renewals allowed, 3rd -> RENEWAL_LIMIT_REACHED")
    void renewalLimit() {
        library.renewLoan(loan, DUE.minusDays(1));
        library.renewLoan(loan, loan.getDueDate().minusDays(1));
        assertEquals(2, loan.getRenewCount());
        LoanRefusedException e = assertThrows(LoanRefusedException.class,
                () -> library.renewLoan(loan, loan.getDueDate().minusDays(1)));
        assertEquals(RefusalReason.RENEWAL_LIMIT_REACHED, e.getReason());
    }

    // Member owes 13 TL from an earlier loan -> renewal of another loan is refused.
    @Test
    @DisplayName("TC-REN-05 member owing >= 10 TL cannot renew -> UNPAID_FINES")
    void renewWithFines() {
        library.addBook(new Book("9783161484100", "Second Book", 1));
        Loan other = library.borrowBook(ID, "9783161484100", BORROWED);
        library.returnBook(other, DUE.plusDays(10));             // 13 TL fine
        LoanRefusedException e = assertThrows(LoanRefusedException.class,
                () -> library.renewLoan(loan, DUE.minusDays(1)));
        assertEquals(RefusalReason.UNPAID_FINES, e.getReason());
    }

    // Negative: a loan that was already returned cannot be renewed.
    @Test
    @DisplayName("TC-REN-06 renewing a returned loan is rejected")
    void renewReturnedLoan() {
        library.returnBook(loan, DUE);
        assertThrows(IllegalStateException.class, () -> library.renewLoan(loan, DUE));
    }
}
