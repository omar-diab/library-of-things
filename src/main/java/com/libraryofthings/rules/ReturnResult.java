package com.libraryofthings.rules;

import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.model.Condition;
import com.libraryofthings.validation.Validator;

/**
 * What happens to the money and trust scores when an item is returned (FR4, BR2, BR2b, BR3, BR5).
 *
 * <pre>
 * GOOD:    the late fee is taken from the deposit first and that part goes to the owner;
 *          the rest of the deposit is refunded; any fee above the deposit becomes a fine.
 * DAMAGED: the whole deposit goes to the owner, nothing is refunded,
 *          and the full late fee becomes a fine.
 * </pre>
 */
public final class ReturnResult {

    private final int daysLate;
    private final int lateFee;
    private final int depositRefunded;
    private final int depositToOwner;
    private final int fineAdded;
    private final int trustChange;
    private final int ownerTrustChange;

    private ReturnResult(int daysLate, int lateFee, int depositRefunded,
                         int depositToOwner, int fineAdded, int trustChange, int ownerTrustChange) {
        this.daysLate = daysLate;
        this.lateFee = lateFee;
        this.depositRefunded = depositRefunded;
        this.depositToOwner = depositToOwner;
        this.fineAdded = fineAdded;
        this.trustChange = trustChange;
        this.ownerTrustChange = ownerTrustChange;
    }

    /** Works out the result of a return. */
    public static ReturnResult settle(int daysLate, int depositHeld, Condition condition) {
        Validator.requireNotNull(condition, "Condition");
        if (depositHeld < 0) {
            throw new InvalidInputException("Deposit held cannot be negative: " + depositHeld);
        }
        int fee = FineCalculator.lateFee(daysLate);
        int trust = TrustPolicy.trustChange(daysLate, condition);
        int ownerTrust = TrustPolicy.ownerReward(daysLate, condition);

        if (condition == Condition.DAMAGED) {
            return new ReturnResult(daysLate, fee, 0, depositHeld, fee, trust, ownerTrust);
        }
        int keptFromDeposit = Math.min(fee, depositHeld);
        int refund = depositHeld - keptFromDeposit;
        int fine = fee - keptFromDeposit;
        return new ReturnResult(daysLate, fee, refund, keptFromDeposit, fine, trust, ownerTrust);
    }

    public int getDaysLate() {
        return daysLate;
    }

    public int getLateFee() {
        return lateFee;
    }

    public int getDepositRefunded() {
        return depositRefunded;
    }

    public int getDepositToOwner() {
        return depositToOwner;
    }

    public int getFineAdded() {
        return fineAdded;
    }

    public int getTrustChange() {
        return trustChange;
    }

    /** Trust points the item's owner earns from this return (BR2b). */
    public int getOwnerTrustChange() {
        return ownerTrustChange;
    }
}
