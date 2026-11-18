package com.libraryofthings.model;

import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.validation.Validator;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * One borrowing of one item by one member (FR3).
 * A loan is active until the item is returned.
 */
public class Loan {

    public static final int MAX_RENEWALS = 1;

    private final Item item;
    private final Member borrower;
    private final LocalDate startDate;
    private final int loanDays;
    private final int depositHeld;
    private LocalDate dueDate;
    private int renewals;
    private LocalDate returnDate;

    public Loan(Item item, Member borrower, LocalDate startDate, int loanDays, int depositHeld) {
        Validator.requireNotNull(item, "Item");
        Validator.requireNotNull(borrower, "Borrower");
        Validator.requireDate(startDate, "Start date");
        Validator.requireLoanDays(loanDays);
        if (depositHeld < 0) {
            throw new InvalidInputException("Deposit held cannot be negative: " + depositHeld);
        }
        this.item = item;
        this.borrower = borrower;
        this.startDate = startDate;
        this.loanDays = loanDays;
        this.depositHeld = depositHeld;
        this.dueDate = startDate.plusDays(loanDays);
        this.renewals = 0;
        this.returnDate = null;
    }

    public boolean isActive() {
        return returnDate == null;
    }

    /** True when the given date is after the due date (on the due date is not overdue). */
    public boolean isOverdueOn(LocalDate date) {
        Validator.requireDate(date, "Date");
        return date.isAfter(dueDate);
    }

    /** Number of days after the due date; 0 if on or before the due date. */
    public int daysLateOn(LocalDate date) {
        Validator.requireDate(date, "Date");
        if (date.isBefore(startDate)) {
            throw new InvalidInputException("Date " + date + " is before the start date " + startDate);
        }
        long days = ChronoUnit.DAYS.between(dueDate, date);
        return (int) Math.max(0, days);
    }

    public boolean canBeRenewedAgain() {
        return renewals < MAX_RENEWALS;
    }

    /** Extends the due date by the original loan length (FR5). Rule checks are done by the service. */
    public void renew() {
        dueDate = dueDate.plusDays(loanDays);
        renewals++;
    }

    /** Marks the loan as returned on the given date (FR4). */
    public void close(LocalDate date) {
        if (!isActive()) {
            throw new InvalidInputException("Loan was already returned on " + returnDate);
        }
        Validator.requireDate(date, "Return date");
        if (date.isBefore(startDate)) {
            throw new InvalidInputException("Return date " + date + " is before the start date " + startDate);
        }
        returnDate = date;
    }

    public Item getItem() {
        return item;
    }

    public Member getBorrower() {
        return borrower;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public int getLoanDays() {
        return loanDays;
    }

    public int getDepositHeld() {
        return depositHeld;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public int getRenewals() {
        return renewals;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }
}
