package com.libraryofthings.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.model.Condition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("TrustPolicy (BR2)")
class TrustPolicyTest {

    /**
     * Decision table: lateness group x condition.
     *
     * <pre>
     * lateness      GOOD   DAMAGED
     * on time        +5     -30
     * 1-7 days      -10     -40
     * 8+ days       -20     -50
     * </pre>
     * Days 1, 7, 8 are also the boundaries between the lateness groups.
     */
    @ParameterizedTest(name = "{0} day(s) late, {1} -> {2}")
    @DisplayName("Decision table + BVA: trust change for every lateness group and condition")
    @CsvSource({
        "0,  GOOD,    5",
        "1,  GOOD,    -10",
        "7,  GOOD,    -10",
        "8,  GOOD,    -20",
        "30, GOOD,    -20",
        "0,  DAMAGED, -30",
        "1,  DAMAGED, -40",
        "7,  DAMAGED, -40",
        "8,  DAMAGED, -50"
    })
    void trustChange(int daysLate, Condition condition, int expected) {
        assertEquals(expected, TrustPolicy.trustChange(daysLate, condition));
    }

    @ParameterizedTest(name = "{0} day(s) late, {1} -> owner {2}")
    @DisplayName("Decision table + BVA: owner reward (BR2b) only for on time and GOOD")
    @CsvSource({
        "0, GOOD,    2",
        "1, GOOD,    0",
        "0, DAMAGED, 0",
        "8, DAMAGED, 0"
    })
    void ownerReward(int daysLate, Condition condition, int expected) {
        assertEquals(expected, TrustPolicy.ownerReward(daysLate, condition));
    }

    @Test
    @DisplayName("Negative: owner reward rejects negative days and a missing condition")
    void ownerRewardInvalidInput() {
        assertThrows(InvalidInputException.class, () -> TrustPolicy.ownerReward(-1, Condition.GOOD));
        assertThrows(InvalidInputException.class, () -> TrustPolicy.ownerReward(0, null));
    }

    @Test
    @DisplayName("Negative: negative days late are rejected")
    void negativeDaysAreRejected() {
        assertThrows(InvalidInputException.class, () -> TrustPolicy.trustChange(-1, Condition.GOOD));
    }

    @Test
    @DisplayName("Negative: a missing condition is rejected")
    void nullConditionIsRejected() {
        assertThrows(InvalidInputException.class, () -> TrustPolicy.trustChange(0, null));
    }
}
