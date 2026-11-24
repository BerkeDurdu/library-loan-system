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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for FR-08 (paying a fine). Valid payment range: 0.01 .. outstanding fine.
 * In every test the member starts with a 13.00 TL fine (book returned 10 days late).
 */
class FinePaymentTest {

    private static final String ID = "M0001";
    private static final String ISBN = "9780306406157";
    private static final LocalDate BORROWED = LocalDate.of(2026, 11, 2);

    private LibraryService library;

    @BeforeEach
    void setUp() {
        StubFineCalculator stub = new StubFineCalculator();
        stub.willReturn("13.00");
        library = new LibraryService(stub);
        library.registerMember(new Member(ID, "Ayse Yilmaz", MemberType.STUDENT));
        library.addBook(new Book(ISBN, "Software Testing", 1));
        Loan loan = library.borrowBook(ID, ISBN, BORROWED);
        library.returnBook(loan, loan.getDueDate().plusDays(10));
    }

    private BigDecimal balance() {
        return library.findMember(ID).getOutstandingFine();
    }

    // Partial payment (a value from the middle of the valid range) reduces the balance.
    @Test
    @DisplayName("TC-PAY-01 partial payment of 5 TL leaves 8 TL")
    void partialPayment() {
        library.payFine(ID, new BigDecimal("5.00"));
        assertEquals(new BigDecimal("8.00"), balance());
    }

    // BVA lower boundary: the smallest possible payment (1 kurus) is accepted.
    @Test
    @DisplayName("TC-PAY-02 BVA: payment of 0.01 TL is accepted")
    void smallestPayment() {
        library.payFine(ID, new BigDecimal("0.01"));
        assertEquals(new BigDecimal("12.99"), balance());
    }

    // BVA upper boundary: paying exactly the full amount clears the balance,
    // and after that the member can borrow again (link to decision table rule C2).
    @Test
    @DisplayName("TC-PAY-03 BVA: paying exactly the full fine clears the balance")
    void fullPayment() {
        library.payFine(ID, new BigDecimal("13.00"));
        assertEquals(new BigDecimal("0.00"), balance());
        assertDoesNotThrow(() -> library.borrowBook(ID, ISBN, BORROWED.plusDays(40)));
    }

    // BVA just above the upper boundary: paying more than owed must be refused,
    // otherwise the balance would become negative (credit). This test found defect D7.
    @Tag("regression")
    @Test
    @DisplayName("TC-PAY-04 BVA: paying 13.01 TL (more than owed) is rejected (D7)")
    void overpaymentRejected() {
        assertThrows(IllegalArgumentException.class, () -> library.payFine(ID, new BigDecimal("13.01")));
        assertEquals(new BigDecimal("13.00"), balance(), "balance must not change after a rejected payment");
    }

    // Negative: zero and negative amounts are not payments.
    @ParameterizedTest(name = "TC-PAY-05 payment of {0} TL is rejected")
    @ValueSource(strings = {"0", "0.00", "-5.00"})
    void zeroOrNegativePayment(String amount) {
        assertThrows(IllegalArgumentException.class, () -> library.payFine(ID, new BigDecimal(amount)));
    }

    @ParameterizedTest(name = "TC-PAY-06 null payment is rejected")
    @NullSource
    void nullPayment(BigDecimal amount) {
        assertThrows(IllegalArgumentException.class, () -> library.payFine(ID, amount));
    }

    @Test
    @DisplayName("TC-PAY-07 payment for an unknown member is rejected")
    void unknownMember() {
        assertThrows(NoSuchElementException.class, () -> library.payFine("M4040", new BigDecimal("1.00")));
    }
}
