package com.svv.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for FR-06 (late fine).
 *
 * Rule: days 1..7 -> 1 TL/day, days 8..30 -> 2 TL/day, day 31+ -> 3 TL/day, maximum 100 TL.
 * Partitions of "days late":  <0 (invalid) | 0 | 1..7 | 8..30 | 31..45 | 46+ (capped)
 * The cap starts between 45 days (98 TL) and 46 days (101 TL -> 100 TL).
 */
class FineCalculatorTest {

    private FineCalculator calculator;

    // Fixture: a new calculator before every test, so no test depends on another.
    @BeforeEach
    void setUp() {
        calculator = new FineCalculator();
    }

    // Data-driven BVA test: the same assertion runs once for every row.
    // For each boundary I test the value ON it and the values just below/above it.
    // Expected values are calculated by hand from the rule above, e.g.
    //   8 days  = 7*1 + 1*2          = 9
    //   30 days = 7*1 + 23*2         = 53
    //   31 days = 53 + 1*3           = 56
    //   46 days = 53 + 16*3 = 101    -> capped to 100
    @ParameterizedTest(name = "TC-FIN-01 BVA: {0} days late -> {1} TL")
    @CsvSource({
            "0,   0.00",   // no delay (lower boundary of the valid domain)
            "1,   1.00",   // first day of tier 1
            "6,   6.00",   // just below the end of tier 1
            "7,   7.00",   // last day of tier 1
            "8,   9.00",   // first day of tier 2  (found D1)
            "9,  11.00",   // just above start of tier 2
            "29, 51.00",   // just below end of tier 2
            "30, 53.00",   // last day of tier 2
            "31, 56.00",   // first day of tier 3
            "45, 98.00",   // last value below the cap
            "46, 100.00",  // first value that hits the cap  (found D2)
            "47, 100.00"   // above the cap
    })
    void fineBoundaries(long daysLate, String expected) {
        assertEquals(new BigDecimal(expected), calculator.calculateFine(daysLate));
    }

    // EP: one typical value from the middle of each tier (not on a boundary).
    @ParameterizedTest(name = "TC-FIN-02 EP: {0} days late -> {1} TL")
    @CsvSource({"4, 4.00", "15, 23.00", "38, 77.00"})
    void fineRepresentatives(long daysLate, String expected) {
        assertEquals(new BigDecimal(expected), calculator.calculateFine(daysLate));
    }

    // A very long delay (one year) must still be 100 TL - checks the cap is applied for big inputs.
    @Tag("regression")
    @Test
    @DisplayName("TC-FIN-03 one year late is capped at 100 TL (D2)")
    void capForVeryLateReturn() {
        assertEquals(FineCalculator.MAX_FINE, calculator.calculateFine(365));
    }

    // Tier 2 must start exactly on day 8 - kept as a separate regression test for defect D1.
    @Tag("regression")
    @Test
    @DisplayName("TC-FIN-04 tier 2 starts on day 8 (D1)")
    void tierTwoStartsOnDayEight() {
        BigDecimal day7 = calculator.calculateFine(7);
        BigDecimal day8 = calculator.calculateFine(8);
        assertEquals(new BigDecimal("2.00"), day8.subtract(day7), "day 8 must cost 2 TL more than day 7");
    }

    // Negative test: -1 is just below the valid domain, the method must refuse it.
    @Test
    @DisplayName("TC-FIN-05 negative days late is rejected")
    void negativeDays() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateFine(-1));
    }
}
