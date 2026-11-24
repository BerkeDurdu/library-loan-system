package com.svv.library.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for FR-01 (member registration data).
 *
 * Inputs: member ID (format "M" + 4 digits), name (2..50 characters after trimming), member type.
 */
class MemberTest {

    // ---------- valid classes (EP) ----------

    // A normal student: checks that all fields are stored and the default state is correct
    // (new members are active and owe nothing). assertAll reports every wrong field at once.
    @Test
    @DisplayName("TC-MEM-01 valid student is created with default state")
    void validStudent() {
        Member m = new Member("M0001", "Ali Kaya", MemberType.STUDENT);
        assertAll(
                () -> assertEquals("M0001", m.getId()),
                () -> assertEquals("Ali Kaya", m.getName()),
                () -> assertEquals(MemberType.STUDENT, m.getType()),
                () -> assertTrue(m.isActive()),
                () -> assertEquals(new BigDecimal("0.00"), m.getOutstandingFine()));
    }

    // STAFF is the other valid class of member type. Also checks the limits attached to each type.
    @Test
    @DisplayName("TC-MEM-02 staff member has 5 loans / 30 days, student 3 / 14")
    void memberTypeLimits() {
        Member staff = new Member("M9999", "Dr. Osman", MemberType.STAFF);
        assertAll(
                () -> assertEquals(5, staff.getType().getMaxActiveLoans()),
                () -> assertEquals(30, staff.getType().getLoanPeriodDays()),
                () -> assertEquals(3, MemberType.STUDENT.getMaxActiveLoans()),
                () -> assertEquals(14, MemberType.STUDENT.getLoanPeriodDays()));
    }

    // ---------- invalid ID classes (EP + negative) ----------

    // Every value breaks the ID format in a different way:
    // lowercase m, 3 digits, 5 digits, wrong letter, letter in the digits, space inside.
    @ParameterizedTest(name = "TC-MEM-03 invalid member ID \"{0}\" is rejected")
    @ValueSource(strings = {"m0001", "M001", "M00001", "X0001", "M00A1", "M 001"})
    void invalidIds(String id) {
        assertThrows(IllegalArgumentException.class, () -> new Member(id, "Ali Kaya", MemberType.STUDENT));
    }

    @ParameterizedTest(name = "TC-MEM-04 null / empty member ID is rejected")
    @NullAndEmptySource
    void nullOrEmptyId(String id) {
        assertThrows(IllegalArgumentException.class, () -> new Member(id, "Ali Kaya", MemberType.STUDENT));
    }

    // ---------- Boundary Value Analysis on name length (valid range 2..50) ----------

    // 1 character = just below the lower boundary -> rejected
    @Test
    @DisplayName("TC-MEM-05 BVA: name with 1 character is rejected")
    void nameLength1() {
        assertThrows(IllegalArgumentException.class, () -> new Member("M0001", "A", MemberType.STUDENT));
    }

    // 2 characters = on the lower boundary -> accepted
    @Test
    @DisplayName("TC-MEM-06 BVA: name with 2 characters is accepted")
    void nameLength2() {
        assertEquals("Al", new Member("M0001", "Al", MemberType.STUDENT).getName());
    }

    // 50 characters = on the upper boundary -> accepted
    @Test
    @DisplayName("TC-MEM-07 BVA: name with 50 characters is accepted")
    void nameLength50() {
        String name = "A".repeat(50);
        assertEquals(name, new Member("M0001", name, MemberType.STUDENT).getName());
    }

    // 51 characters = just above the upper boundary -> rejected
    @Test
    @DisplayName("TC-MEM-08 BVA: name with 51 characters is rejected")
    void nameLength51() {
        assertThrows(IllegalArgumentException.class,
                () -> new Member("M0001", "A".repeat(51), MemberType.STUDENT));
    }

    // ---------- negative tests ----------

    // A name made only of spaces has length 3 but is not a real name.
    // This test found defect D6 in the first version (spaces were counted as characters).
    @Tag("regression")
    @Test
    @DisplayName("TC-MEM-09 name made only of spaces is rejected (D6)")
    void blankNameRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Member("M0001", "   ", MemberType.STUDENT));
    }

    // Leading/trailing spaces should be removed before saving, and the length rule uses the trimmed name.
    // " A " is 3 characters with spaces but only 1 real character -> must be rejected.
    @Tag("regression")
    @Test
    @DisplayName("TC-MEM-10 name is trimmed before the length check (D6)")
    void nameIsTrimmed() {
        assertAll(
                () -> assertEquals("Ali", new Member("M0001", "  Ali  ", MemberType.STUDENT).getName()),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Member("M0002", " A ", MemberType.STUDENT)));
    }

    @Test
    @DisplayName("TC-MEM-11 null name is rejected")
    void nullName() {
        assertThrows(IllegalArgumentException.class, () -> new Member("M0001", null, MemberType.STUDENT));
    }

    @Test
    @DisplayName("TC-MEM-12 null member type is rejected")
    void nullType() {
        assertThrows(NullPointerException.class, () -> new Member("M0001", "Ali Kaya", null));
    }
}
