package com.svv.library.service;

/** Thrown when a business rule refuses a borrow or renew request. */
public class LoanRefusedException extends RuntimeException {

    private final RefusalReason reason;

    public LoanRefusedException(RefusalReason reason) {
        super("Request refused: " + reason);
        this.reason = reason;
    }

    public RefusalReason getReason() {
        return reason;
    }
}
