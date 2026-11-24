package com.svv.library.service;

import com.svv.library.model.Money;

import java.math.BigDecimal;

/**
 * Calculates the late-return fine (FR-06).
 * <pre>
 *   days 1  .. 7   : 1.00 TL per day
 *   days 8  .. 30  : 2.00 TL per day
 *   days 31 and on : 3.00 TL per day
 *   total fine is capped at 100.00 TL
 * </pre>
 */
public class FineCalculator {

    public static final BigDecimal MAX_FINE = Money.of("100.00");
    private static final int TIER1_END = 7;
    private static final int TIER2_END = 30;

    public BigDecimal calculateFine(long daysLate) {
        if (daysLate < 0) {
            throw new IllegalArgumentException("Days late cannot be negative");
        }
        long amount;
        if (daysLate <= TIER1_END) {
            amount = daysLate;
        } else if (daysLate <= TIER2_END) {
            // fix D1: tier 2 days are counted from the end of tier 1 (was "daysLate - 8")
            amount = TIER1_END + (daysLate - TIER1_END) * 2;
        } else {
            amount = TIER1_END + (TIER2_END - TIER1_END) * 2 + (daysLate - TIER2_END) * 3;
        }
        // fix D2: the 100 TL maximum was defined but never applied
        return Money.of(BigDecimal.valueOf(amount)).min(MAX_FINE);
    }
}
