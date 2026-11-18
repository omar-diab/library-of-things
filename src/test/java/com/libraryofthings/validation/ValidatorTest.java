package com.libraryofthings.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.libraryofthings.exception.InvalidInputException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Validator (BR8)")
class ValidatorTest {

    @ParameterizedTest(name = "\"{0}\" is a valid member ID")
    @DisplayName("EP: valid member IDs")
    @ValueSource(strings = {"M0001", "M9999", "M0000"})
    void validMemberId(String id) {
        assertDoesNotThrow(() -> Validator.requireMemberId(id));
    }

    @ParameterizedTest(name = "\"{0}\" is an invalid member ID")
    @DisplayName("EP / BVA: invalid member IDs (too short, too long, wrong letter, letters)")
    @NullAndEmptySource
    @ValueSource(strings = {"M001", "M00001", "m0001", "X0001", "I0001", "M00A1", " M0001", "M0001 "})
    void invalidMemberId(String id) {
        assertThrows(InvalidInputException.class, () -> Validator.requireMemberId(id));
    }

    @ParameterizedTest(name = "\"{0}\" is a valid item ID")
    @DisplayName("EP: valid item IDs")
    @ValueSource(strings = {"I0001", "I9999"})
    void validItemId(String id) {
        assertDoesNotThrow(() -> Validator.requireItemId(id));
    }

    @ParameterizedTest(name = "\"{0}\" is an invalid item ID")
    @DisplayName("EP / BVA: invalid item IDs")
    @NullAndEmptySource
    @ValueSource(strings = {"I001", "I00001", "i0001", "M0001", "I-001"})
    void invalidItemId(String id) {
        assertThrows(InvalidInputException.class, () -> Validator.requireItemId(id));
    }

    @ParameterizedTest(name = "loan of {0} day(s) is valid")
    @DisplayName("BVA: loan length starts at 1 day")
    @ValueSource(ints = {1, 2, 14})
    void validLoanDays(int days) {
        assertDoesNotThrow(() -> Validator.requireLoanDays(days));
    }

    @ParameterizedTest(name = "loan of {0} day(s) is invalid")
    @DisplayName("BVA / negative: loan length below 1 day")
    @ValueSource(ints = {0, -1})
    void invalidLoanDays(int days) {
        assertThrows(InvalidInputException.class, () -> Validator.requireLoanDays(days));
    }

    @Test
    @DisplayName("Negative: a null date is rejected")
    void nullDateIsRejected() {
        assertThrows(InvalidInputException.class, () -> Validator.requireDate(null, "Start date"));
        assertDoesNotThrow(() -> Validator.requireDate(LocalDate.of(2026, 11, 18), "Start date"));
    }
}
