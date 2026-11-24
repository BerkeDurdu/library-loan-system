package com.svv.library.model;

/**
 * Member categories. Each category has its own loan limit and loan period (FR-03, FR-04).
 */
public enum MemberType {
    STUDENT(3, 14),
    STAFF(5, 30);

    private final int maxActiveLoans;
    private final int loanPeriodDays;

    MemberType(int maxActiveLoans, int loanPeriodDays) {
        this.maxActiveLoans = maxActiveLoans;
        this.loanPeriodDays = loanPeriodDays;
    }

    public int getMaxActiveLoans() {
        return maxActiveLoans;
    }

    public int getLoanPeriodDays() {
        return loanPeriodDays;
    }
}
