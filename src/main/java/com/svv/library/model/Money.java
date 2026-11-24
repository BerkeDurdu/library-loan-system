package com.svv.library.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Small helper so that all money values use 2 decimal places (BigDecimal avoids double rounding errors).
 */
public final class Money {

    public static final BigDecimal ZERO = of(BigDecimal.ZERO);

    private Money() {
    }

    public static BigDecimal of(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String value) {
        return of(new BigDecimal(value));
    }
}
