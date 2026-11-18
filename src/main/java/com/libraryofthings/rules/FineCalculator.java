package com.libraryofthings.rules;

import com.libraryofthings.exception.InvalidInputException;

/**
 * Late fee rule (BR3).
 *
 * <pre>
 * days late (d)   fee
 * d = 0           0 TL
 * 1 to 7          5 TL per day
 * 8 or more       35 + 10 TL per day after day 7, capped at 150 TL
 * </pre>
 */
public final class FineCalculator {

    public static final int FIRST_WEEK_DAYS = 7;
    public static final int FIRST_WEEK_RATE = 5;
    public static final int LATER_RATE = 10;
    public static final int MAX_FEE = 150;

    private FineCalculator() {
        // Utility class: only static methods.
    }

    public static int lateFee(int daysLate) {
        if (daysLate < 0) {
            throw new InvalidInputException("Days late cannot be negative: " + daysLate);
        }
        if (daysLate <= FIRST_WEEK_DAYS) {
            return daysLate * FIRST_WEEK_RATE;
        }
        int firstWeekFee = FIRST_WEEK_DAYS * FIRST_WEEK_RATE;
        int laterFee = (daysLate - FIRST_WEEK_DAYS) * LATER_RATE;
        return Math.min(firstWeekFee + laterFee, MAX_FEE);
    }
}
