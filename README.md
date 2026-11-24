# Library Loan Management System

SWE411 Software Verification and Validation – research assignment.
A small Java 17 application (no GUI, no database) that manages library members, books, loans,
renewals and late fines, tested with JUnit 5 and measured with JaCoCo.

## Requirements
- JDK 17 or newer
- Maven 3.9+

## Commands
```
mvn clean test                  # run all 103 tests + coverage report (target/site/jacoco/index.html)
mvn test -Dgroups=regression    # run only the regression-tagged tests
mvn compile exec:java           # run the console demo
```

## Structure
```
src/main/java/com/svv/library
  model/    Member, MemberType, Book, Loan, Money
  util/     IsbnValidator
  service/  LibraryService, FineCalculator, RefusalReason, LoanRefusedException
  Main.java console demo
src/test/java/com/svv/library
  util/IsbnValidatorTest          FR-02  EP, BVA, negative
  model/MemberTest                FR-01  EP, BVA, negative, regression
  model/BookTest                  FR-02  EP, BVA, negative
  service/FineCalculatorTest      FR-06  BVA, EP, negative, regression
  service/BorrowDecisionTableTest FR-03  decision table, BVA, negative
  service/ReturnAndRenewTest      FR-05, FR-07  BVA, decision table, negative
  service/FinePaymentTest         FR-08  BVA, negative, regression
  service/StubFineCalculator      test double used by the service tests
test-results/
  run1-initial-version.txt   first run: 103 tests, 17 failures, 1 error (7 defects)
  run2-after-fixes.txt       after fixes: 103 tests, all pass
  run3-regression-only.txt   regression subset (11 tests)
  surefire-reports/          JUnit reports
  jacoco-report/index.html   coverage report
  demo-output.txt            console demo output
```
