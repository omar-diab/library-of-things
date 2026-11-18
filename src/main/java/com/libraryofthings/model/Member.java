package com.libraryofthings.model;

import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.validation.Validator;

/**
 * A community member who can lend and borrow items (FR1).
 * A new member starts with a trust score of 50 and no unpaid fines.
 */
public class Member {

    public static final int STARTING_TRUST_SCORE = 50;

    private final String id;
    private final String name;
    private int trustScore;
    private int unpaidFines;

    public Member(String id, String name) {
        Validator.requireMemberId(id);
        Validator.requireName(name, "Member name");
        this.id = id;
        this.name = name;
        this.trustScore = STARTING_TRUST_SCORE;
        this.unpaidFines = 0;
    }

    /** Current tier, worked out from the trust score (BR1). */
    public TrustTier getTier() {
        return TrustTier.fromScore(trustScore);
    }

    /**
     * Adds (positive) or removes (negative) trust points.
     * The score is kept between 0 and 100 (BR2).
     */
    public void changeTrust(int delta) {
        int newScore = trustScore + delta;
        if (newScore > TrustTier.MAX_SCORE) {
            newScore = TrustTier.MAX_SCORE;
        }
        if (newScore < TrustTier.MIN_SCORE) {
            newScore = TrustTier.MIN_SCORE;
        }
        trustScore = newScore;
    }

    /** Adds a fine to the member's balance. */
    public void addFine(int amount) {
        if (amount < 0) {
            throw new InvalidInputException("Fine cannot be negative: " + amount);
        }
        unpaidFines += amount;
    }

    /** Pays part or all of the unpaid fines (FR6). */
    public void payFine(int amount) {
        if (amount <= 0) {
            throw new InvalidInputException("Payment must be more than 0 TL: " + amount);
        }
        if (amount > unpaidFines) {
            throw new InvalidInputException(
                    "Payment " + amount + " TL is more than the unpaid fines " + unpaidFines + " TL");
        }
        unpaidFines -= amount;
    }

    public boolean hasUnpaidFines() {
        return unpaidFines > 0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTrustScore() {
        return trustScore;
    }

    public int getUnpaidFines() {
        return unpaidFines;
    }
}
