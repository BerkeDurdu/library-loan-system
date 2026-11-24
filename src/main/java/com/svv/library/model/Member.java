package com.svv.library.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A library member (FR-01). Holds the member's identity, category, status and unpaid fine balance.
 */
public class Member {

    /** Member IDs look like M0001 ... M9999. */
    private static final Pattern ID_PATTERN = Pattern.compile("M\\d{4}");
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 50;

    private final String id;
    private final String name;
    private final MemberType type;
    private boolean active;
    private BigDecimal outstandingFine;

    public Member(String id, String name, MemberType type) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Member ID must match M + 4 digits, e.g. M0001");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
        // fix D6: trim first, so "   " or " A " cannot pass the length rule
        String trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Name length must be between 2 and 50 characters");
        }
        this.id = id;
        this.name = trimmed;
        this.type = Objects.requireNonNull(type, "Member type must not be null");
        this.active = true;
        this.outstandingFine = Money.ZERO;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MemberType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public BigDecimal getOutstandingFine() {
        return outstandingFine;
    }

    void addFine(BigDecimal amount) {
        outstandingFine = Money.of(outstandingFine.add(amount));
    }

    void reduceFine(BigDecimal amount) {
        outstandingFine = Money.of(outstandingFine.subtract(amount));
    }

    @Override
    public String toString() {
        return id + " " + name + " (" + type + ")";
    }
}
