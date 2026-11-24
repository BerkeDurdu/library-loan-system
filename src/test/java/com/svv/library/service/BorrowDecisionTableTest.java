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

import java.time.LocalDate;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for FR-03 / FR-04 (borrowing a book) using a DECISION TABLE.
 *
 * Conditions                           R1  R2  R3  R4  R5  R6
 *  C1 member is active                  T   F   T   T   T   T
 *  C2 unpaid fine < 10.00 TL            T   -   F   T   T   T
 *  C3 active loans < limit (3 / 5)      T   -   -   F   T   T
 *  C4 a copy is available               T   -   -   -   F   T
 *  C5 member does not have this book    T   -   -   -   -   F
 * Action
 *  loan created                         X
 *  refused with reason                      INACTIVE / UNPAID_FINES / LIMIT / NO_COPY / ALREADY
 * ("-" = don't care, because an earlier rule already decides the outcome)
 */
class BorrowDecisionTableTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);
    private static final String STUDENT = "M0001";
    private static final String STAFF = "M0002";
    // six different books so a member can hold several loans
    private static final String[] ISBNS = {
            "9780306406157", "9783161484100", "9780143007234",
            "9791090636071", "9780131103627", "9780201633610"};
    private static final String FINE_BOOK = "9780596009205";

    private LibraryService library;
    private StubFineCalculator fineStub;

    // Fixture (prefix values): a fresh library with one student, one staff member and 7 books.
    // Runs before EVERY test so tests are independent of each other.
    @BeforeEach
    void setUp() {
        fineStub = new StubFineCalculator();
        library = new LibraryService(fineStub);
        library.registerMember(new Member(STUDENT, "Ayse Yilmaz", MemberType.STUDENT));
        library.registerMember(new Member(STAFF, "Mehmet Demir", MemberType.STAFF));
        for (String isbn : ISBNS) {
            library.addBook(new Book(isbn, "Book " + isbn, 2));
        }
        library.addBook(new Book(FINE_BOOK, "Book used to create fines", 5));
    }

    /** Helper: makes the member owe exactly the given amount (borrow + late return with the stub). */
    private void giveFine(String memberId, String amount) {
        fineStub.willReturn(amount);
        Loan loan = library.borrowBook(memberId, FINE_BOOK, TODAY.minusDays(40));
        library.returnBook(loan, TODAY);
    }

    /** Helper: member borrows the first n books from ISBNS. */
    private void borrowBooks(String memberId, int n) {
        for (int i = 0; i < n; i++) {
            library.borrowBook(memberId, ISBNS[i], TODAY);
        }
    }

    // ---------- R1: every condition true -> loan is created ----------

    // Checks the result of a successful borrow: due date = today + 14 for students (FR-04),
    // one copy less on the shelf, and the loan appears in the member's active loans.
    @Test
    @DisplayName("TC-BOR-01 R1: all conditions true -> loan created, due in 14 days")
    void rule1AllConditionsTrue() {
        Loan loan = library.borrowBook(STUDENT, ISBNS[0], TODAY);
        assertAll(
                () -> assertEquals(TODAY.plusDays(14), loan.getDueDate()),
                () -> assertEquals(1, library.findBook(ISBNS[0]).getAvailableCopies()),
                () -> assertEquals(1, library.getActiveLoans(STUDENT).size()),
                () -> assertEquals(0, loan.getRenewCount()));
    }

    // Same rule for a staff member: the loan period is 30 days.
    @Test
    @DisplayName("TC-BOR-02 R1: staff loan is due in 30 days")
    void staffLoanPeriod() {
        assertEquals(TODAY.plusDays(30), library.borrowBook(STAFF, ISBNS[0], TODAY).getDueDate());
    }

    // ---------- R2: inactive member ----------

    @Test
    @DisplayName("TC-BOR-03 R2: inactive member is refused")
    void rule2InactiveMember() {
        library.findMember(STUDENT).setActive(false);
        LoanRefusedException e = assertThrows(LoanRefusedException.class,
                () -> library.borrowBook(STUDENT, ISBNS[0], TODAY));
        assertEquals(RefusalReason.MEMBER_INACTIVE, e.getReason());
    }

    // Rule priority: member is inactive AND has fines. The table says C1 is checked first,
    // so the reason must be MEMBER_INACTIVE, not UNPAID_FINES.
    @Test
    @DisplayName("TC-BOR-04 R2 priority: inactive + fines -> MEMBER_INACTIVE")
    void inactiveHasPriorityOverFines() {
        giveFine(STUDENT, "50.00");
        library.findMember(STUDENT).setActive(false);
        LoanRefusedException e = assertThrows(LoanRefusedException.class,
                () -> library.borrowBook(STUDENT, ISBNS[0], TODAY));
        assertEquals(RefusalReason.MEMBER_INACTIVE, e.getReason());
    }

    // ---------- R3: unpaid fines (BVA around the 10.00 TL limit) ----------

    // 9.99 is just below the limit -> borrowing is still allowed.
    @Test
    @DisplayName("TC-BOR-05 R3 BVA: fine 9.99 TL -> allowed")
    void fineJustBelowLimit() {
        giveFine(STUDENT, "9.99");
        assertDoesNotThrow(() -> library.borrowBook(STUDENT, ISBNS[0], TODAY));
    }

    // 10.00 is exactly on the limit. Requirement says "10.00 TL or more blocks borrowing".
    // This test found defect D4 (the code used "> 10" instead of ">= 10").
    // 10.01 is just above, 50.00 is a normal value from the invalid partition.
    @Tag("regression")
    @ParameterizedTest(name = "TC-BOR-06 R3 BVA: fine {0} TL -> UNPAID_FINES (D4)")
    @CsvSource({"10.00", "10.01", "50.00"})
    void fineOnOrAboveLimit(String fine) {
        giveFine(STUDENT, fine);
        LoanRefusedException e = assertThrows(LoanRefusedException.class,
                () -> library.borrowBook(STUDENT, ISBNS[0], TODAY));
        assertEquals(RefusalReason.UNPAID_FINES, e.getReason());
    }

    // ---------- R4: loan limit (BVA around 3 for students, 5 for staff) ----------

    // Student already has 2 loans (limit - 1) -> the 3rd one is allowed.
    @Test
    @DisplayName("TC-BOR-07 R4 BVA: student with 2 loans can borrow a 3rd")
    void studentBelowLimit() {
        borrowBooks(STUDENT, 2);
        assertDoesNotThrow(() -> library.borrowBook(STUDENT, ISBNS[2], TODAY));
        assertEquals(3, library.getActiveLoans(STUDENT).size());
    }

    // Student already has 3 loans (= limit) -> the 4th must be refused.
    // This test found defect D3 (the code used "size > limit", so a 4th loan was possible).
    @Tag("regression")
    @Test
    @DisplayName("TC-BOR-08 R4 BVA: student with 3 loans is refused (D3)")
    void studentAtLimit() {
        borrowBooks(STUDENT, 3);
        LoanRefusedException e = assertThrows(LoanRefusedException.class,
                () -> library.borrowBook(STUDENT, ISBNS[3], TODAY));
        assertEquals(RefusalReason.LOAN_LIMIT_REACHED, e.getReason());
    }

    // Same boundary for staff (limit 5): 4 loans -> ok, 5 loans -> refused.
    @Tag("regression")
    @Test
    @DisplayName("TC-BOR-09 R4 BVA: staff can hold 5 loans but not 6 (D3)")
    void staffLimit() {
        borrowBooks(STAFF, 4);
        assertDoesNotThrow(() -> library.borrowBook(STAFF, ISBNS[4], TODAY));
        LoanRefusedException e = assertThrows(LoanRefusedException.class,
                () -> library.borrowBook(STAFF, ISBNS[5], TODAY));
        assertEquals(RefusalReason.LOAN_LIMIT_REACHED, e.getReason());
    }

    // ---------- R5: no copy available ----------

    // Book has 2 copies; staff borrows one, student borrows the other, then a third person asks.
    @Test
    @DisplayName("TC-BOR-10 R5: all copies are lent out -> NO_COPY_AVAILABLE")
    void rule5NoCopy() {
        library.registerMember(new Member("M0003", "Can Ozturk", MemberType.STUDENT));
        library.borrowBook(STAFF, ISBNS[0], TODAY);
        library.borrowBook(STUDENT, ISBNS[0], TODAY);
        LoanRefusedException e = assertThrows(LoanRefusedException.class,
                () -> library.borrowBook("M0003", ISBNS[0], TODAY));
        assertEquals(RefusalReason.NO_COPY_AVAILABLE, e.getReason());
    }

    // ---------- R6: member already has this book ----------

    @Test
    @DisplayName("TC-BOR-11 R6: same book twice -> ALREADY_BORROWED")
    void rule6SameBookTwice() {
        library.borrowBook(STUDENT, ISBNS[0], TODAY);
        LoanRefusedException e = assertThrows(LoanRefusedException.class,
                () -> library.borrowBook(STUDENT, ISBNS[0], TODAY));
        assertEquals(RefusalReason.ALREADY_BORROWED, e.getReason());
    }

    // ---------- negative tests: wrong input instead of a business rule ----------

    @Test
    @DisplayName("TC-BOR-12 unknown member ID -> NoSuchElementException")
    void unknownMember() {
        assertThrows(NoSuchElementException.class, () -> library.borrowBook("M7777", ISBNS[0], TODAY));
    }

    @Test
    @DisplayName("TC-BOR-13 unknown ISBN -> NoSuchElementException")
    void unknownBook() {
        assertThrows(NoSuchElementException.class, () -> library.borrowBook(STUDENT, "9780000000002", TODAY));
    }

    @Test
    @DisplayName("TC-BOR-14 null date -> IllegalArgumentException")
    void nullDate() {
        assertThrows(IllegalArgumentException.class, () -> library.borrowBook(STUDENT, ISBNS[0], null));
    }

    // Registering the same member ID or the same ISBN twice must be refused (FR-01, FR-02).
    @Test
    @DisplayName("TC-BOR-15 duplicate member / duplicate book is rejected")
    void duplicates() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> library.registerMember(new Member(STUDENT, "Other Name", MemberType.STAFF))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> library.addBook(new Book("978-0-306-40615-7", "Same ISBN", 1))));
    }
}
