package com.libraryofthings.model;

import com.libraryofthings.exception.InvalidInputException;

/**
 * Borrowing tiers based on a member's trust score (BR1).
 *
 * <pre>
 * Tier      Score    Max loans  Max days  Deposit paid
 * BLOCKED   0-29     0          0         -
 * STANDARD  30-69    2          7         100%
 * TRUSTED   70-100   5          14        50%
 * </pre>
 */
public enum TrustTier {

    BLOCKED(0, 29, 0, 0, 100),
    STANDARD(30, 69, 2, 7, 100),
    TRUSTED(70, 100, 5, 14, 50);

    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 100;

    private final int minScore;
    private final int maxScore;
    private final int maxActiveLoans;
    private final int maxLoanDays;
    private final int depositPercent;

    TrustTier(int minScore, int maxScore, int maxActiveLoans, int maxLoanDays, int depositPercent) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.maxActiveLoans = maxActiveLoans;
        this.maxLoanDays = maxLoanDays;
        this.depositPercent = depositPercent;
    }

    /** Returns the tier that contains the given score (0-100). */
    public static TrustTier fromScore(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new InvalidInputException("Trust score must be between 0 and 100: " + score);
        }
        for (TrustTier tier : values()) {
            if (score >= tier.minScore && score <= tier.maxScore) {
                return tier;
            }
        }
        throw new IllegalStateException("No tier covers score " + score);
    }

    public boolean canBorrow() {
        return this != BLOCKED;
    }

    /**
     * The deposit this tier pays for an item, rounded down to whole TL.
     * Example: TRUSTED pays 50%, so an item with a 75 TL deposit costs 37 TL.
     */
    public int depositFor(int itemDeposit) {
        return itemDeposit * depositPercent / 100;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public int getMaxActiveLoans() {
        return maxActiveLoans;
    }

    public int getMaxLoanDays() {
        return maxLoanDays;
    }

    public int getDepositPercent() {
        return depositPercent;
    }
}
