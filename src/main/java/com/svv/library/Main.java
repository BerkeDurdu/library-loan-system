package com.svv.library;

import com.svv.library.model.Book;
import com.svv.library.model.Loan;
import com.svv.library.model.Member;
import com.svv.library.model.MemberType;
import com.svv.library.service.LibraryService;
import com.svv.library.service.LoanRefusedException;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Small console demo that walks through a typical scenario (no GUI / database needed). */
public class Main {

    public static void main(String[] args) {
        LibraryService library = new LibraryService();
        library.registerMember(new Member("M0001", "Ayse Yilmaz", MemberType.STUDENT));
        library.addBook(new Book("978-0-306-40615-7", "Software Testing Basics", 2));

        LocalDate day1 = LocalDate.of(2026, 11, 2);
        Loan loan = library.borrowBook("M0001", "9780306406157", day1);
        System.out.println("Borrowed: " + loan.getBook().getTitle() + ", due " + loan.getDueDate());

        BigDecimal fine = library.returnBook(loan, loan.getDueDate().plusDays(10));
        System.out.println("Returned 10 days late, fine = " + fine + " TL");

        try {
            library.borrowBook("M0001", "9780306406157", day1.plusDays(30));
        } catch (LoanRefusedException e) {
            System.out.println("Second borrow refused: " + e.getReason());
        }

        library.payFine("M0001", fine);
        System.out.println("After payment, balance = "
                + library.findMember("M0001").getOutstandingFine() + " TL");
    }
}
