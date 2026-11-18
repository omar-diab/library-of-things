package com.libraryofthings.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.libraryofthings.exception.BorrowNotAllowedException;
import com.libraryofthings.exception.BorrowRefusal;
import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.exception.RenewalNotAllowedException;
import com.libraryofthings.exception.RenewalRefusal;
import com.libraryofthings.model.Category;
import com.libraryofthings.model.Condition;
import com.libraryofthings.model.ItemStatus;
import com.libraryofthings.model.Loan;
import com.libraryofthings.model.Member;
import com.libraryofthings.model.TrustTier;
import com.libraryofthings.rules.ReturnResult;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Returning and renewing (FR4, FR5)")
class ReturnAndRenewTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 2);
    private static final LocalDate DUE = LocalDate.of(2026, 11, 9); // START + 7 days
    private static final String OWNER = "M0001";
    private static final String BORROWER = "M0002";
    private static final String OTHER = "M0003";
    private static final String DRILL = "I0001";

    private LendingService service;
    private Member borrower;

    @BeforeEach
    void setUp() {
        service = new LendingService();
        service.registerMember(OWNER, "Owner");
        borrower = service.registerMember(BORROWER, "Borrower");
        service.registerMember(OTHER, "Other");
        service.addItem(DRILL, "Drill", Category.TOOLS, OWNER, 200);
        service.borrow(BORROWER, DRILL, START, 7);
    }

    @Nested
    @DisplayName("Returning an item (FR4)")
    class Returning {

        @ParameterizedTest(name = "returned {0}, {1}: refund {2}, fine {3}, trust {4}, item {5}")
        @DisplayName("End-to-end: deposit 200, trust 50, returned on different days and conditions")
        @CsvSource({
            "2026-11-05, GOOD,    200, 0,   55, AVAILABLE",
            "2026-11-09, GOOD,    200, 0,   55, AVAILABLE",
            "2026-11-10, GOOD,    195, 0,   40, AVAILABLE",
            "2026-11-17, GOOD,    155, 0,   30, AVAILABLE",
            "2026-12-31, GOOD,    50,  0,   30, AVAILABLE",
            "2026-11-09, DAMAGED, 0,   0,   20, WITHDRAWN",
            "2026-11-17, DAMAGED, 0,   45,  0,  WITHDRAWN"
        })
        void returnOutcome(LocalDate returnDate, Condition condition, int refund, int fine,
                           int trustAfter, ItemStatus statusAfter) {
            ReturnResult result = service.returnItem(DRILL, returnDate, condition);

            assertEquals(refund, result.getDepositRefunded());
            assertEquals(fine, borrower.getUnpaidFines());
            assertEquals(trustAfter, borrower.getTrustScore());
            assertEquals(statusAfter, service.getItem(DRILL).getStatus());
            assertTrue(service.activeLoans(BORROWER).isEmpty());
        }

        @Test
        @DisplayName("A cheap item returned very late leaves a fine (fee above deposit)")
        void feeAboveDeposit() {
            service.addItem("I0002", "Spoon", Category.KITCHEN, OWNER, 20);
            service.borrow(OTHER, "I0002", START, 7);
            ReturnResult result = service.returnItem("I0002", DUE.plusDays(8), Condition.GOOD); // fee 45

            assertEquals(20, result.getDepositToOwner());
            assertEquals(25, service.getMember(OTHER).getUnpaidFines());
        }

        @Test
        @DisplayName("Being late twice lowers trust to BLOCKED, and then borrowing is refused")
        void repeatedLatenessBlocks() {
            service.returnItem(DRILL, DUE.plusDays(8), Condition.GOOD);  // 50 - 20 = 30
            service.borrow(BORROWER, DRILL, DUE.plusDays(8), 7);
            service.returnItem(DRILL, DUE.plusDays(16), Condition.GOOD); // 30 - 10 = 20

            assertEquals(TrustTier.BLOCKED, borrower.getTier());
            BorrowNotAllowedException e = assertThrows(BorrowNotAllowedException.class,
                    () -> service.borrow(BORROWER, DRILL, DUE.plusDays(20), 7));
            assertEquals(BorrowRefusal.LOW_TRUST, e.getReason());
        }

        @Test
        @DisplayName("A reservation stays on the item after it is returned")
        void reservationSurvivesReturn() {
            service.reserve(OTHER, DRILL);
            service.returnItem(DRILL, DUE, Condition.GOOD);
            assertEquals(OTHER, service.getItem(DRILL).getReservedBy());
        }

        @Test
        @DisplayName("Negative: returning an item that is not borrowed")
        void notBorrowed() {
            service.returnItem(DRILL, DUE, Condition.GOOD);
            assertThrows(InvalidInputException.class, () -> service.returnItem(DRILL, DUE, Condition.GOOD));
        }

        @Test
        @DisplayName("Negative / BVA: return date one day before the start date is rejected; start date is accepted")
        void returnBeforeStart() {
            assertThrows(InvalidInputException.class,
                    () -> service.returnItem(DRILL, START.minusDays(1), Condition.GOOD));
            assertEquals(ItemStatus.BORROWED, service.getItem(DRILL).getStatus());
            service.returnItem(DRILL, START, Condition.GOOD);
            assertEquals(ItemStatus.AVAILABLE, service.getItem(DRILL).getStatus());
        }

        @Test
        @DisplayName("Negative: missing date or condition")
        void missingInput() {
            assertThrows(InvalidInputException.class, () -> service.returnItem(DRILL, null, Condition.GOOD));
            assertThrows(InvalidInputException.class, () -> service.returnItem(DRILL, DUE, null));
        }
    }

    @Nested
    @DisplayName("Owner earns trust by lending (BR2b, defect D2)")
    class OwnerReward {

        @ParameterizedTest(name = "returned {0}, {1}: owner trust {2}")
        @DisplayName("Decision table + BVA: only an on-time return in good condition rewards the owner (+2)")
        @CsvSource({
            "2026-11-02, GOOD,    52",
            "2026-11-09, GOOD,    52",
            "2026-11-10, GOOD,    50",
            "2026-11-09, DAMAGED, 50",
            "2026-11-17, DAMAGED, 50"
        })
        void ownerReward(LocalDate returnDate, Condition condition, int ownerTrustAfter) {
            service.returnItem(DRILL, returnDate, condition);
            assertEquals(ownerTrustAfter, service.getMember(OWNER).getTrustScore());
        }

        @Test
        @DisplayName("A BLOCKED owner (29) gets back to STANDARD (31) by lending, and can borrow again")
        void blockedOwnerRecovers() {
            Member owner = service.getMember(OWNER);
            owner.changeTrust(-21); // 50 -> 29, BLOCKED
            assertEquals(TrustTier.BLOCKED, owner.getTier());

            service.returnItem(DRILL, DUE, Condition.GOOD);

            assertEquals(31, owner.getTrustScore());
            assertEquals(TrustTier.STANDARD, owner.getTier());
            service.addItem("I0002", "Tent", Category.OUTDOOR, OTHER, 100);
            service.borrow(OWNER, "I0002", DUE, 7);
            assertEquals(1, service.activeLoans(OWNER).size());
        }

        @Test
        @DisplayName("BVA: the owner's reward never takes the score above 100")
        void rewardIsCapped() {
            Member owner = service.getMember(OWNER);
            owner.changeTrust(49); // 99
            service.returnItem(DRILL, DUE, Condition.GOOD);
            assertEquals(100, owner.getTrustScore());
        }
    }

    @Nested
    @DisplayName("Renewing a loan (FR5, BR6)")
    class Renewing {

        /**
         * Decision table for BR6 (T = the blocking condition is present).
         * Rows 1-4: each condition alone. Rows 5-6: combinations that check the fixed order.
         */
        @ParameterizedTest(name = "R{0}: overdue={1} renewedOnce={2} reservedByOther={3} -> {4}")
        @DisplayName("Decision table: the three renewal conditions and their priority")
        @CsvSource({
            "1, false, false, false, ALLOWED",
            "2, true,  false, false, OVERDUE",
            "3, false, true,  false, RENEWAL_LIMIT_REACHED",
            "4, false, false, true,  RESERVED_BY_OTHER",
            "5, true,  true,  true,  OVERDUE",
            "6, false, true,  true,  RENEWAL_LIMIT_REACHED"
        })
        void decisionTable(int rule, boolean overdue, boolean renewedOnce,
                           boolean reservedByOther, String expected) {
            LocalDate dueDate = DUE;
            if (renewedOnce) {
                service.renew(DRILL, START.plusDays(1));
                dueDate = DUE.plusDays(7);
            }
            if (reservedByOther) {
                service.reserve(OTHER, DRILL);
            }
            LocalDate renewalDate = overdue ? dueDate.plusDays(1) : dueDate;

            if (expected.equals("ALLOWED")) {
                Loan loan = service.renew(DRILL, renewalDate);
                assertEquals(DUE.plusDays(7), loan.getDueDate());
                assertEquals(1, loan.getRenewals());
            } else {
                RenewalNotAllowedException e = assertThrows(RenewalNotAllowedException.class,
                        () -> service.renew(DRILL, renewalDate));
                assertEquals(RenewalRefusal.valueOf(expected), e.getReason());
            }
        }

        @ParameterizedTest(name = "renew on {0}: allowed = {1}")
        @DisplayName("BVA: renewing on the due date is allowed, the day after is not")
        @CsvSource({
            "2026-11-08, true",
            "2026-11-09, true",
            "2026-11-10, false"
        })
        void renewalDateBoundary(LocalDate date, boolean allowed) {
            if (allowed) {
                assertEquals(DUE.plusDays(7), service.renew(DRILL, date).getDueDate());
            } else {
                RenewalNotAllowedException e = assertThrows(RenewalNotAllowedException.class,
                        () -> service.renew(DRILL, date));
                assertEquals(RenewalRefusal.OVERDUE, e.getReason());
            }
        }

        @ParameterizedTest(name = "renew on {0}: rejected = {1}")
        @DisplayName("BVA / negative: a renewal date before the start date (Nov 2) is rejected (defect D1)")
        @CsvSource({
            "2026-01-01, true",
            "2026-11-01, true",
            "2026-11-02, false"
        })
        void renewalBeforeStart(LocalDate date, boolean rejected) {
            if (rejected) {
                assertThrows(InvalidInputException.class, () -> service.renew(DRILL, date));
                Loan loan = service.activeLoans(BORROWER).get(0);
                assertEquals(DUE, loan.getDueDate());
                assertEquals(0, loan.getRenewals());
            } else {
                assertEquals(DUE.plusDays(7), service.renew(DRILL, date).getDueDate());
            }
        }

        @Test
        @DisplayName("After a renewal, returning on the new due date is on time")
        void returnAfterRenewal() {
            service.renew(DRILL, DUE);
            ReturnResult result = service.returnItem(DRILL, DUE.plusDays(7), Condition.GOOD);
            assertEquals(0, result.getDaysLate());
            assertEquals(200, result.getDepositRefunded());
        }

        @Test
        @DisplayName("Negative: renewing an item that is not borrowed")
        void notBorrowed() {
            service.returnItem(DRILL, DUE, Condition.GOOD);
            assertThrows(InvalidInputException.class, () -> service.renew(DRILL, DUE));
        }

        @Test
        @DisplayName("A refused renewal does not change the due date")
        void refusalHasNoSideEffects() {
            service.reserve(OTHER, DRILL);
            assertThrows(RenewalNotAllowedException.class, () -> service.renew(DRILL, DUE));
            Loan loan = service.activeLoans(BORROWER).get(0);
            assertEquals(DUE, loan.getDueDate());
            assertFalse(loan.getRenewals() > 0);
        }
    }
}
