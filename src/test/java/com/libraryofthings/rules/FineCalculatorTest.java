package com.libraryofthings.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.libraryofthings.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("FineCalculator (BR3)")
class FineCalculatorTest {

    @ParameterizedTest(name = "{0} day(s) late -> {1} TL")
    @DisplayName("BVA: fee at, below and above each boundary (0, 7/8, 18/19)")
    @CsvSource({
        "0,   0",
        "1,   5",
        "6,   30",
        "7,   35",
        "8,   45",
        "9,   55",
        "17,  135",
        "18,  145",
        "19,  150",
        "20,  150",
        "365, 150"
    })
    void lateFee(int daysLate, int expectedFee) {
        assertEquals(expectedFee, FineCalculator.lateFee(daysLate));
    }

    @ParameterizedTest(name = "{0} day(s) late is rejected")
    @DisplayName("BVA / negative: negative days late are invalid")
    @ValueSource(ints = {-1, -30})
    void negativeDaysAreRejected(int daysLate) {
        assertThrows(InvalidInputException.class, () -> FineCalculator.lateFee(daysLate));
    }
}
