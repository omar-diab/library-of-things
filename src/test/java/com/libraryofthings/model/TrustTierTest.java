package com.libraryofthings.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.libraryofthings.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("TrustTier (BR1)")
class TrustTierTest {

    @ParameterizedTest(name = "score {0} -> {1}")
    @DisplayName("BVA: scores at each tier boundary map to the right tier")
    @CsvSource({
        "0,   BLOCKED",
        "29,  BLOCKED",
        "30,  STANDARD",
        "69,  STANDARD",
        "70,  TRUSTED",
        "100, TRUSTED"
    })
    void scoreMapsToTier(int score, TrustTier expected) {
        assertEquals(expected, TrustTier.fromScore(score));
    }

    @ParameterizedTest(name = "score {0} is rejected")
    @DisplayName("BVA / negative: scores just outside 0-100 are invalid")
    @ValueSource(ints = {-1, 101})
    void scoreOutsideRangeIsRejected(int score) {
        assertThrows(InvalidInputException.class, () -> TrustTier.fromScore(score));
    }

    @Test
    @DisplayName("Only BLOCKED members cannot borrow")
    void onlyBlockedCannotBorrow() {
        assertFalse(TrustTier.BLOCKED.canBorrow());
        assertTrue(TrustTier.STANDARD.canBorrow());
        assertTrue(TrustTier.TRUSTED.canBorrow());
    }

    @ParameterizedTest(name = "{0}: max loans {1}, max days {2}")
    @DisplayName("Each tier has the limits from the requirements")
    @CsvSource({
        "BLOCKED,  0, 0",
        "STANDARD, 2, 7",
        "TRUSTED,  5, 14"
    })
    void tierLimits(TrustTier tier, int maxLoans, int maxDays) {
        assertEquals(maxLoans, tier.getMaxActiveLoans());
        assertEquals(maxDays, tier.getMaxLoanDays());
    }

    @ParameterizedTest(name = "{0} pays {2} TL for a {1} TL deposit")
    @DisplayName("Deposit: STANDARD pays 100%, TRUSTED pays 50% rounded down")
    @CsvSource({
        "STANDARD, 0,    0",
        "STANDARD, 200,  200",
        "STANDARD, 1000, 1000",
        "TRUSTED,  0,    0",
        "TRUSTED,  200,  100",
        "TRUSTED,  75,   37",
        "TRUSTED,  1,    0",
        "TRUSTED,  1000, 500"
    })
    void depositForTier(TrustTier tier, int itemDeposit, int expected) {
        assertEquals(expected, tier.depositFor(itemDeposit));
    }
}
