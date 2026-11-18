package com.libraryofthings.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.libraryofthings.exception.InvalidInputException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Loan")
class LoanTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private static final LocalDate DUE = LocalDate.of(2026, 11, 8);

    private Item drill;
    private Member borrower;
    private Loan loan;

    @BeforeEach
    void setUp() {
        drill = new Item("I0001", "Drill", Category.TOOLS, "M0001", 200);
        borrower = new Member("M0002", "Sara");
        loan = new Loan(drill, borrower, START, 7, 200);
    }

    @Nested
    @DisplayName("Creating a loan")
    class Creation {

        @Test
        @DisplayName("Due date = start date + loan length; the loan is active")
        void dueDateAndDefaults() {
            assertEquals(DUE, loan.getDueDate());
            assertTrue(loan.isActive());
            assertEquals(0, loan.getRenewals());
            assertEquals(200, loan.getDepositHeld());
            assertNull(loan.getReturnDate());
        }

        @Test
        @DisplayName("BVA: a 1-day loan is due the next day")
        void oneDayLoan() {
            Loan shortLoan = new Loan(drill, borrower, START, 1, 0);
            assertEquals(START.plusDays(1), shortLoan.getDueDate());
        }

        @Test
        @DisplayName("Due date crosses a month end correctly")
        void dueDateAcrossMonthEnd() {
            Loan monthEnd = new Loan(drill, borrower, LocalDate.of(2026, 11, 28), 7, 0);
            assertEquals(LocalDate.of(2026, 12, 5), monthEnd.getDueDate());
        }

        @ParameterizedTest(name = "loan length {0} is rejected")
        @DisplayName("BVA / negative: loan length below 1 day")
        @ValueSource(ints = {0, -1})
        void invalidLength(int days) {
            assertThrows(InvalidInputException.class, () -> new Loan(drill, borrower, START, days, 0));
        }

        @Test
        @DisplayName("Negative: missing item, borrower or start date, or a negative deposit")
        void invalidArguments() {
            assertThrows(InvalidInputException.class, () -> new Loan(null, borrower, START, 7, 0));
            assertThrows(InvalidInputException.class, () -> new Loan(drill, null, START, 7, 0));
            assertThrows(InvalidInputException.class, () -> new Loan(drill, borrower, null, 7, 0));
            assertThrows(InvalidInputException.class, () -> new Loan(drill, borrower, START, 7, -1));
        }
    }

    @Nested
    @DisplayName("Lateness")
    class Lateness {

        @ParameterizedTest(name = "on {0}: {1} day(s) late, overdue = {2}")
        @DisplayName("BVA around the due date (Nov 8)")
        @CsvSource({
            "2026-11-01, 0, false",
            "2026-11-07, 0, false",
            "2026-11-08, 0, false",
            "2026-11-09, 1, true",
            "2026-11-15, 7, true",
            "2026-11-16, 8, true"
        })
        void daysLateAndOverdue(LocalDate date, int expectedDaysLate, boolean expectedOverdue) {
            assertEquals(expectedDaysLate, loan.daysLateOn(date));
            assertEquals(expectedOverdue, loan.isOverdueOn(date));
        }

        @Test
        @DisplayName("Negative: a date before the start date is rejected")
        void dateBeforeStart() {
            assertThrows(InvalidInputException.class, () -> loan.daysLateOn(START.minusDays(1)));
        }
    }

    @Nested
    @DisplayName("Renewing and closing")
    class RenewAndClose {

        @Test
        @DisplayName("Renewing adds the original loan length to the due date")
        void renewExtendsDueDate() {
            assertTrue(loan.canBeRenewedAgain());
            loan.renew();
            assertEquals(DUE.plusDays(7), loan.getDueDate());
            assertEquals(1, loan.getRenewals());
            assertFalse(loan.canBeRenewedAgain());
        }

        @Test
        @DisplayName("Closing records the return date and ends the loan")
        void closeEndsLoan() {
            loan.close(DUE);
            assertFalse(loan.isActive());
            assertEquals(DUE, loan.getReturnDate());
        }

        @Test
        @DisplayName("BVA: returning on the start date is allowed")
        void closeOnStartDate() {
            loan.close(START);
            assertEquals(START, loan.getReturnDate());
        }

        @Test
        @DisplayName("Negative: returning before the start date is rejected")
        void closeBeforeStart() {
            assertThrows(InvalidInputException.class, () -> loan.close(START.minusDays(1)));
            assertTrue(loan.isActive());
        }

        @Test
        @DisplayName("Negative: a loan cannot be returned twice")
        void closeTwice() {
            loan.close(DUE);
            assertThrows(InvalidInputException.class, () -> loan.close(DUE));
        }
    }
}
