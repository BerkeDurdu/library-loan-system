package com.svv.library.service;

/** Why a borrow or renew request was refused (outcomes of the decision tables). */
public enum RefusalReason {
    MEMBER_INACTIVE,
    UNPAID_FINES,
    LOAN_LIMIT_REACHED,
    NO_COPY_AVAILABLE,
    ALREADY_BORROWED,
    RENEWAL_LIMIT_REACHED,
    LOAN_OVERDUE
}
