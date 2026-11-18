package com.libraryofthings.rules;

import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.model.Condition;
import com.libraryofthings.validation.Validator;

/**
 * How a member's trust score changes when they return an item (BR2).
 *
 * <pre>
 * returned on time and in good condition   +5
 * 1 to 7 days late                          -10
 * 8 or more days late                       -20
 * damaged                                   -30 (added to any late change)
 *
 * owner, when their item comes back on time in good condition   +2 (BR2b)
 * </pre>
 */
public final class TrustPolicy {

    public static final int ON_TIME_BONUS = 5;
    public static final int SHORT_LATE_PENALTY = -10;
    public static final int LONG_LATE_PENALTY = -20;
    public static final int DAMAGE_PENALTY = -30;
    public static final int LONG_LATE_FROM_DAY = 8;
    public static final int OWNER_REWARD = 2;

    private TrustPolicy() {
        // Utility class: only static methods.
    }

    public static int trustChange(int daysLate, Condition condition) {
        if (daysLate < 0) {
            throw new InvalidInputException("Days late cannot be negative: " + daysLate);
        }
        Validator.requireNotNull(condition, "Condition");

        int change;
        if (daysLate == 0) {
            change = (condition == Condition.GOOD) ? ON_TIME_BONUS : 0;
        } else if (daysLate < LONG_LATE_FROM_DAY) {
            change = SHORT_LATE_PENALTY;
        } else {
            change = LONG_LATE_PENALTY;
        }

        if (condition == Condition.DAMAGED) {
            change += DAMAGE_PENALTY;
        }
        return change;
    }

    /** BR2b: the owner earns trust when their item is returned on time and in good condition. */
    public static int ownerReward(int daysLate, Condition condition) {
        if (daysLate < 0) {
            throw new InvalidInputException("Days late cannot be negative: " + daysLate);
        }
        Validator.requireNotNull(condition, "Condition");
        return (daysLate == 0 && condition == Condition.GOOD) ? OWNER_REWARD : 0;
    }
}
