package com.libraryofthings.validation;

import com.libraryofthings.exception.InvalidInputException;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Input validation rules (BR8). Every method throws {@link InvalidInputException}
 * when the value is not valid and returns normally otherwise.
 */
public final class Validator {

    public static final int MIN_DEPOSIT = 0;
    public static final int MAX_DEPOSIT = 1000;
    public static final int MIN_LOAN_DAYS = 1;

    private static final Pattern MEMBER_ID = Pattern.compile("M[0-9]{4}");
    private static final Pattern ITEM_ID = Pattern.compile("I[0-9]{4}");

    private Validator() {
        // Utility class: only static methods, no objects.
    }

    public static void requireMemberId(String id) {
        if (id == null || !MEMBER_ID.matcher(id).matches()) {
            throw new InvalidInputException("Member ID must be 'M' followed by 4 digits: " + id);
        }
    }

    public static void requireItemId(String id) {
        if (id == null || !ITEM_ID.matcher(id).matches()) {
            throw new InvalidInputException("Item ID must be 'I' followed by 4 digits: " + id);
        }
    }

    public static void requireName(String name, String fieldName) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException(fieldName + " must not be empty");
        }
    }

    public static void requireDeposit(int deposit) {
        if (deposit < MIN_DEPOSIT || deposit > MAX_DEPOSIT) {
            throw new InvalidInputException(
                    "Deposit must be between " + MIN_DEPOSIT + " and " + MAX_DEPOSIT + " TL: " + deposit);
        }
    }

    public static void requireLoanDays(int days) {
        if (days < MIN_LOAN_DAYS) {
            throw new InvalidInputException("Loan length must be at least " + MIN_LOAN_DAYS + " day: " + days);
        }
    }

    public static void requireDate(LocalDate date, String fieldName) {
        if (date == null) {
            throw new InvalidInputException(fieldName + " must not be null");
        }
    }

    public static void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new InvalidInputException(fieldName + " must not be null");
        }
    }
}
