package com.svv.library.service;

import com.svv.library.model.Money;

import java.math.BigDecimal;

/**
 * Test double (stub) for FineCalculator.
 *
 * In the borrow/renew tests I only want to control "how much fine does the member owe".
 * With the real calculator the fine would depend on the fine formula (which has its own tests),
 * so a bug there would also break the borrow tests. This stub always returns the amount I set,
 * which improves controllability (lecture 3: testability = controllability + observability).
 */
class StubFineCalculator extends FineCalculator {

    private BigDecimal nextFine = Money.ZERO;

    void willReturn(String amount) {
        nextFine = Money.of(amount);
    }

    @Override
    public BigDecimal calculateFine(long daysLate) {
        return nextFine;
    }
}
