package com.libraryofthings.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.model.Condition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("ReturnResult – deposit settlement (FR4)")
class ReturnResultTest {

    /**
     * Decision table on two conditions: item condition, and late fee compared with the deposit.
     * The fee/deposit rows also test the boundary where the fee equals the deposit (45 vs 45).
     */
    @ParameterizedTest(name = "{0}, deposit {1}, {2} days late -> fee {3}, refund {4}, owner {5}, fine {6}")
    @DisplayName("Decision table: condition x (late fee vs deposit)")
    @CsvSource({
        // condition, deposit, daysLate, fee, refund, toOwner, fine
        "GOOD,    100, 0,  0,   100, 0,   0",
        "GOOD,    100, 7,  35,  65,  35,  0",
        "GOOD,    46,  8,  45,  1,   45,  0",
        "GOOD,    45,  8,  45,  0,   45,  0",
        "GOOD,    44,  8,  45,  0,   44,  1",
        "GOOD,    0,   19, 150, 0,   0,   150",
        "DAMAGED, 100, 0,  0,   0,   100, 0",
        "DAMAGED, 100, 8,  45,  0,   100, 45",
        "DAMAGED, 0,   19, 150, 0,   0,   150"
    })
    void settlement(Condition condition, int deposit, int daysLate,
                    int fee, int refund, int toOwner, int fine) {
        ReturnResult result = ReturnResult.settle(daysLate, deposit, condition);

        assertEquals(daysLate, result.getDaysLate());
        assertEquals(fee, result.getLateFee());
        assertEquals(refund, result.getDepositRefunded());
        assertEquals(toOwner, result.getDepositToOwner());
        assertEquals(fine, result.getFineAdded());
    }

    @Test
    @DisplayName("Money is conserved: refund + owner share = deposit, owner share + fine = fee (GOOD)")
    void moneyIsConserved() {
        ReturnResult result = ReturnResult.settle(9, 50, Condition.GOOD);
        assertEquals(50, result.getDepositRefunded() + result.getDepositToOwner());
        assertEquals(result.getLateFee(), result.getDepositToOwner() + result.getFineAdded());
    }

    @Test
    @DisplayName("The borrower and owner trust changes from TrustPolicy are included")
    void includesTrustChange() {
        assertEquals(5, ReturnResult.settle(0, 100, Condition.GOOD).getTrustChange());
        assertEquals(-50, ReturnResult.settle(8, 100, Condition.DAMAGED).getTrustChange());
        assertEquals(2, ReturnResult.settle(0, 100, Condition.GOOD).getOwnerTrustChange());
        assertEquals(0, ReturnResult.settle(1, 100, Condition.GOOD).getOwnerTrustChange());
    }

    @Test
    @DisplayName("Negative: a negative deposit or missing condition is rejected")
    void invalidInput() {
        assertThrows(InvalidInputException.class, () -> ReturnResult.settle(0, -1, Condition.GOOD));
        assertThrows(InvalidInputException.class, () -> ReturnResult.settle(0, 100, null));
        assertThrows(InvalidInputException.class, () -> ReturnResult.settle(-1, 100, Condition.GOOD));
    }
}
