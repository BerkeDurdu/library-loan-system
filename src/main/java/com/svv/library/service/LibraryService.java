package com.svv.library.service;

import com.svv.library.model.Book;
import com.svv.library.model.Loan;
import com.svv.library.model.Member;
import com.svv.library.model.Money;
import com.svv.library.util.IsbnValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Main business logic: registration, borrowing, returning, renewing and paying fines.
 * Dates are always passed in as parameters (instead of LocalDate.now()) so the tests can control time.
 */
public class LibraryService {

    /** A member owing this amount or more cannot borrow or renew (FR-03, FR-07). */
    public static final BigDecimal FINE_BLOCK_LIMIT = Money.of("10.00");
    public static final int MAX_RENEWALS = 2;

    private final Map<String, Member> members = new LinkedHashMap<>();
    private final Map<String, Book> books = new LinkedHashMap<>();
    private final List<Loan> loans = new ArrayList<>();
    private final FineCalculator fineCalculator;

    public LibraryService() {
        this(new FineCalculator());
    }

    public LibraryService(FineCalculator fineCalculator) {
        this.fineCalculator = fineCalculator;
    }

    // ---------------------------------------------------------------- FR-01 / FR-02 registration

    public void registerMember(Member member) {
        if (members.containsKey(member.getId())) {
            throw new IllegalArgumentException("Member already registered: " + member.getId());
        }
        members.put(member.getId(), member);
    }

    public void addBook(Book book) {
        if (books.containsKey(book.getIsbn())) {
            throw new IllegalArgumentException("Book already in catalogue: " + book.getIsbn());
        }
        books.put(book.getIsbn(), book);
    }

    public Member findMember(String memberId) {
        Member member = members.get(memberId);
        if (member == null) {
            throw new NoSuchElementException("Unknown member: " + memberId);
        }
        return member;
    }

    public Book findBook(String isbn) {
        Book book = isbn == null ? null : books.get(IsbnValidator.normalize(isbn));
        if (book == null) {
            throw new NoSuchElementException("Unknown book: " + isbn);
        }
        return book;
    }

    public List<Loan> getActiveLoans(String memberId) {
        List<Loan> result = new ArrayList<>();
        for (Loan loan : loans) {
            if (!loan.isReturned() && loan.getMember().getId().equals(memberId)) {
                result.add(loan);
            }
        }
        return Collections.unmodifiableList(result);
    }

    // ---------------------------------------------------------------- FR-03 / FR-04 borrowing

    /**
     * Borrow decision table, rules are checked in this order:
     * inactive member -> unpaid fines -> loan limit -> no copy -> same book already borrowed.
     */
    public Loan borrowBook(String memberId, String isbn, LocalDate date) {
        requireDate(date);
        Member member = findMember(memberId);
        Book book = findBook(isbn);

        if (!member.isActive()) {
            throw new LoanRefusedException(RefusalReason.MEMBER_INACTIVE);
        }
        if (isBlockedByFines(member)) {
            throw new LoanRefusedException(RefusalReason.UNPAID_FINES);
        }
        List<Loan> active = getActiveLoans(memberId);
        // fix D3: the limit is reached when size == max (was "size > max", allowing one extra loan)
        if (active.size() >= member.getType().getMaxActiveLoans()) {
            throw new LoanRefusedException(RefusalReason.LOAN_LIMIT_REACHED);
        }
        if (!book.hasAvailableCopy()) {
            throw new LoanRefusedException(RefusalReason.NO_COPY_AVAILABLE);
        }
        for (Loan loan : active) {
            if (loan.getBook().getIsbn().equals(book.getIsbn())) {
                throw new LoanRefusedException(RefusalReason.ALREADY_BORROWED);
            }
        }
        Loan loan = Loan.open(member, book, date);
        loans.add(loan);
        return loan;
    }

    // ---------------------------------------------------------------- FR-05 returning

    /** Returns the book and charges the late fine. Returns the fine of this loan. */
    public BigDecimal returnBook(Loan loan, LocalDate returnDate) {
        requireDate(returnDate);
        if (loan.isReturned()) {
            throw new IllegalStateException("Loan has already been returned");
        }
        if (returnDate.isBefore(loan.getBorrowDate())) {
            throw new IllegalArgumentException("Return date cannot be before the borrow date");
        }
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate(), returnDate));
        BigDecimal fine = fineCalculator.calculateFine(daysLate);
        loan.close(returnDate, fine);
        return fine;
    }

    // ---------------------------------------------------------------- FR-07 renewing

    /** Renewal decision table: returned? -> overdue? -> renew limit -> unpaid fines. */
    public void renewLoan(Loan loan, LocalDate date) {
        requireDate(date);
        if (loan.isReturned()) {
            throw new IllegalStateException("A returned loan cannot be renewed");
        }
        // fix D5: renewing ON the due date is allowed, only dates after it are overdue
        if (date.isAfter(loan.getDueDate())) {
            throw new LoanRefusedException(RefusalReason.LOAN_OVERDUE);
        }
        if (loan.getRenewCount() >= MAX_RENEWALS) {
            throw new LoanRefusedException(RefusalReason.RENEWAL_LIMIT_REACHED);
        }
        if (isBlockedByFines(loan.getMember())) {
            throw new LoanRefusedException(RefusalReason.UNPAID_FINES);
        }
        loan.extend();
    }

    // ---------------------------------------------------------------- FR-08 paying fines

    public void payFine(String memberId, BigDecimal amount) {
        Member member = findMember(memberId);
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment must be greater than zero");
        }
        // fix D7: a payment larger than the debt would make the balance negative
        if (amount.compareTo(member.getOutstandingFine()) > 0) {
            throw new IllegalArgumentException("Payment is larger than the outstanding fine");
        }
        Loan.applyPayment(member, Money.of(amount));
    }

    /**
     * fix D4: one shared rule for borrow AND renew. Borrow used "> 10" while renew used ">= 10";
     * the requirement says a member owing 10.00 TL or more is blocked.
     */
    private static boolean isBlockedByFines(Member member) {
        return member.getOutstandingFine().compareTo(FINE_BLOCK_LIMIT) >= 0;
    }

    private static void requireDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date must not be null");
        }
    }
}
