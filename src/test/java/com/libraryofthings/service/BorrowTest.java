package com.libraryofthings.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.libraryofthings.exception.BorrowNotAllowedException;
import com.libraryofthings.exception.BorrowRefusal;
import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.exception.NotFoundException;
import com.libraryofthings.model.Category;
import com.libraryofthings.model.Condition;
import com.libraryofthings.model.ItemStatus;
import com.libraryofthings.model.Loan;
import com.libraryofthings.model.Member;
import com.libraryofthings.model.TrustTier;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Borrowing an item (FR3, BR4)")
class BorrowTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 2);
    private static final String OWNER = "M0001";
    private static final String BORROWER = "M0002";
    private static final String OTHER_OWNER = "M0003";
    private static final String OTHER_BORROWER = "M0004";
    private static final String DRILL = "I0001";

    private LendingService service;

    @BeforeEach
    void setUp() {
        service = new LendingService();
        service.registerMember(OWNER, "Owner");
        service.registerMember(BORROWER, "Borrower");
        service.registerMember(OTHER_OWNER, "Other owner");
        service.registerMember(OTHER_BORROWER, "Other borrower");
        service.addItem(DRILL, "Drill", Category.TOOLS, OWNER, 200);
        for (int i = 1; i <= 6; i++) {
            service.addItem("I010" + i, "Filler " + i, Category.OTHER, OTHER_OWNER, 10);
        }
    }

    /**
     * Decision table for BR4. Each row is one rule (column) of the table.
     * T = the condition that blocks borrowing is present.
     *
     * Rows 1-7 test each condition alone. Rows 8-12 combine several conditions
     * to check the fixed order: the first failing check is the reason reported.
     */
    @ParameterizedTest(name = "R{0}: own={1} lowTrust={2} fines={3} atLimit={4} tooLong={5} unavailable={6} -> {7}")
    @DisplayName("Decision table: the six borrowing conditions and their priority")
    @CsvSource({
        // rule, own,  lowTrust, fines, atLimit, tooLong, unavailable, expected
        "1,  false, false, false, false, false, false, ALLOWED",
        "2,  true,  false, false, false, false, false, OWN_ITEM",
        "3,  false, true,  false, false, false, false, LOW_TRUST",
        "4,  false, false, true,  false, false, false, UNPAID_FINES",
        "5,  false, false, false, true,  false, false, LOAN_LIMIT_REACHED",
        "6,  false, false, false, false, true,  false, LOAN_TOO_LONG",
        "7,  false, false, false, false, false, true,  ITEM_NOT_AVAILABLE",
        "8,  true,  true,  true,  true,  true,  true,  OWN_ITEM",
        "9,  false, true,  true,  true,  true,  true,  LOW_TRUST",
        "10, false, false, true,  true,  true,  true,  UNPAID_FINES",
        "11, false, false, false, true,  true,  true,  LOAN_LIMIT_REACHED",
        "12, false, false, false, false, true,  true,  LOAN_TOO_LONG"
    })
    void decisionTable(int rule, boolean own, boolean lowTrust, boolean fines,
                       boolean atLimit, boolean tooLong, boolean unavailable, String expected) {
        String memberId = own ? OWNER : BORROWER;
        Member member = service.getMember(memberId);

        if (unavailable) {
            service.borrow(OTHER_BORROWER, DRILL, START, 7);
        }
        if (atLimit) {
            service.borrow(memberId, "I0101", START, 7);
            service.borrow(memberId, "I0102", START, 7);
        }
        if (fines) {
            member.addFine(10);
        }
        if (lowTrust) {
            member.changeTrust(-21); // 50 -> 29, BLOCKED
        }
        int days = tooLong ? 8 : 7; // STANDARD maximum is 7 days

        if (expected.equals("ALLOWED")) {
            Loan loan = service.borrow(memberId, DRILL, START, days);
            assertEquals(ItemStatus.BORROWED, service.getItem(DRILL).getStatus());
            assertEquals(START.plusDays(7), loan.getDueDate());
            assertEquals(200, loan.getDepositHeld());
        } else {
            BorrowNotAllowedException e = assertThrows(BorrowNotAllowedException.class,
                    () -> service.borrow(memberId, DRILL, START, days));
            assertEquals(BorrowRefusal.valueOf(expected), e.getReason());
        }
    }

    @Nested
    @DisplayName("Boundary values")
    class Boundaries {

        @ParameterizedTest(name = "trust {0}: allowed = {1}")
        @DisplayName("BVA: trust score 29 is blocked, 30 can borrow")
        @CsvSource({"29, false", "30, true"})
        void trustBoundary(int score, boolean allowed) {
            service.getMember(BORROWER).changeTrust(score - 50);
            assertBorrow(allowed, BorrowRefusal.LOW_TRUST, () -> service.borrow(BORROWER, DRILL, START, 7));
        }

        @ParameterizedTest(name = "{0} active loan(s): allowed = {1}")
        @DisplayName("BVA: STANDARD limit is 2 active loans")
        @CsvSource({"0, true", "1, true", "2, false"})
        void standardLoanLimit(int existingLoans, boolean allowed) {
            borrowFillers(BORROWER, existingLoans);
            assertBorrow(allowed, BorrowRefusal.LOAN_LIMIT_REACHED,
                    () -> service.borrow(BORROWER, DRILL, START, 7));
        }

        @ParameterizedTest(name = "{0} active loan(s): allowed = {1}")
        @DisplayName("BVA: TRUSTED limit is 5 active loans")
        @CsvSource({"4, true", "5, false"})
        void trustedLoanLimit(int existingLoans, boolean allowed) {
            service.getMember(BORROWER).changeTrust(20); // 70, TRUSTED
            borrowFillers(BORROWER, existingLoans);
            assertBorrow(allowed, BorrowRefusal.LOAN_LIMIT_REACHED,
                    () -> service.borrow(BORROWER, DRILL, START, 7));
        }

        @ParameterizedTest(name = "trust {0}, {1} days: allowed = {2}")
        @DisplayName("BVA: loan length limit is 7 days (STANDARD) and 14 days (TRUSTED)")
        @CsvSource({
            "50, 1,  true",
            "50, 7,  true",
            "50, 8,  false",
            "70, 14, true",
            "70, 15, false"
        })
        void loanLengthLimit(int score, int days, boolean allowed) {
            service.getMember(BORROWER).changeTrust(score - 50);
            assertBorrow(allowed, BorrowRefusal.LOAN_TOO_LONG,
                    () -> service.borrow(BORROWER, DRILL, START, days));
        }

        @ParameterizedTest(name = "trust {0} pays {1} TL")
        @DisplayName("BVA: deposit is 100% at score 69 and 50% at score 70")
        @CsvSource({"69, 200", "70, 100"})
        void depositByTier(int score, int expectedDeposit) {
            service.getMember(BORROWER).changeTrust(score - 50);
            assertEquals(expectedDeposit, service.borrow(BORROWER, DRILL, START, 7).getDepositHeld());
        }

        @Test
        @DisplayName("BVA: 1 TL of unpaid fines blocks borrowing; paying it unblocks")
        void smallestFineBlocks() {
            service.getMember(BORROWER).addFine(1);
            assertBorrow(false, BorrowRefusal.UNPAID_FINES, () -> service.borrow(BORROWER, DRILL, START, 7));
            service.payFine(BORROWER, 1);
            assertBorrow(true, null, () -> service.borrow(BORROWER, DRILL, START, 7));
        }
    }

    @Nested
    @DisplayName("Item availability")
    class Availability {

        @Test
        @DisplayName("A withdrawn item cannot be borrowed")
        void withdrawnItem() {
            service.withdrawItem(OWNER, DRILL);
            assertBorrow(false, BorrowRefusal.ITEM_NOT_AVAILABLE, () -> service.borrow(BORROWER, DRILL, START, 7));
        }

        @Test
        @DisplayName("An item reserved by someone else cannot be borrowed")
        void reservedByOther() {
            service.borrow(OTHER_BORROWER, DRILL, START, 7);
            service.reserve(BORROWER, DRILL);
            service.returnItem(DRILL, START.plusDays(3), Condition.GOOD);
            assertBorrow(false, BorrowRefusal.ITEM_NOT_AVAILABLE,
                    () -> service.borrow(OTHER_BORROWER, DRILL, START.plusDays(3), 7));
        }

        @Test
        @DisplayName("The member who reserved the item can borrow it, and the reservation is cleared")
        void reservedBySelf() {
            service.borrow(OTHER_BORROWER, DRILL, START, 7);
            service.reserve(BORROWER, DRILL);
            service.returnItem(DRILL, START.plusDays(3), Condition.GOOD);

            service.borrow(BORROWER, DRILL, START.plusDays(3), 7);
            assertNull(service.getItem(DRILL).getReservedBy());
        }

        @Test
        @DisplayName("A borrowed loan appears in the member's active loans")
        void appearsInActiveLoans() {
            Loan loan = service.borrow(BORROWER, DRILL, START, 7);
            assertTrue(service.activeLoans(BORROWER).contains(loan));
            assertEquals(TrustTier.STANDARD, service.getMember(BORROWER).getTier());
        }
    }

    @Nested
    @DisplayName("Invalid requests (negative testing)")
    class Negative {

        @ParameterizedTest(name = "loan length {0} is rejected")
        @DisplayName("Loan length below 1 day")
        @ValueSource(ints = {0, -1})
        void invalidLength(int days) {
            assertThrows(InvalidInputException.class, () -> service.borrow(BORROWER, DRILL, START, days));
        }

        @Test
        @DisplayName("A missing start date is rejected")
        void nullDate() {
            assertThrows(InvalidInputException.class, () -> service.borrow(BORROWER, DRILL, null, 7));
        }

        @Test
        @DisplayName("Unknown member or item")
        void unknownIds() {
            assertThrows(NotFoundException.class, () -> service.borrow("M9999", DRILL, START, 7));
            assertThrows(NotFoundException.class, () -> service.borrow(BORROWER, "I9999", START, 7));
        }

        @Test
        @DisplayName("Badly formatted IDs are invalid input, not 'not found'")
        void badlyFormattedIds() {
            assertThrows(InvalidInputException.class, () -> service.borrow("M12", DRILL, START, 7));
            assertThrows(InvalidInputException.class, () -> service.borrow(BORROWER, "drill", START, 7));
        }

        @Test
        @DisplayName("A refused request changes nothing")
        void refusalHasNoSideEffects() {
            assertThrows(BorrowNotAllowedException.class, () -> service.borrow(BORROWER, DRILL, START, 8));
            assertEquals(ItemStatus.AVAILABLE, service.getItem(DRILL).getStatus());
            assertTrue(service.activeLoans(BORROWER).isEmpty());
        }
    }

    // ---------------------------------------------------------------- helpers

    private void borrowFillers(String memberId, int count) {
        for (int i = 1; i <= count; i++) {
            service.borrow(memberId, "I010" + i, START, 7);
        }
    }

    private void assertBorrow(boolean allowed, BorrowRefusal reason,
                              Executable borrowCall) {
        if (allowed) {
            assertDoesNotThrow(borrowCall);
        } else {
            BorrowNotAllowedException e = assertThrows(BorrowNotAllowedException.class, borrowCall);
            assertEquals(reason, e.getReason());
        }
    }
}
