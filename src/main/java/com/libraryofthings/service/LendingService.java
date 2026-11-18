package com.libraryofthings.service;

import com.libraryofthings.exception.BorrowNotAllowedException;
import com.libraryofthings.exception.BorrowRefusal;
import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.exception.NotFoundException;
import com.libraryofthings.exception.RenewalNotAllowedException;
import com.libraryofthings.exception.RenewalRefusal;
import com.libraryofthings.model.Category;
import com.libraryofthings.model.Condition;
import com.libraryofthings.model.Item;
import com.libraryofthings.model.ItemStatus;
import com.libraryofthings.model.Loan;
import com.libraryofthings.model.Member;
import com.libraryofthings.model.TrustTier;
import com.libraryofthings.rules.ReturnResult;
import com.libraryofthings.validation.Validator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The lending engine: all functional requirements FR1-FR10.
 * Data is kept in memory.
 */
public class LendingService {

    private final Map<String, Member> members = new LinkedHashMap<>();
    private final Map<String, Item> items = new LinkedHashMap<>();
    private final List<Loan> loans = new ArrayList<>();

    // ---------------------------------------------------------------- FR1, FR2

    /** FR1: registers a new member with trust score 50 and no fines. */
    public Member registerMember(String memberId, String name) {
        Validator.requireMemberId(memberId);
        if (members.containsKey(memberId)) {
            throw new InvalidInputException("Member ID already exists: " + memberId);
        }
        Member member = new Member(memberId, name);
        members.put(memberId, member);
        return member;
    }

    /** FR2: adds an item owned by a registered member. */
    public Item addItem(String itemId, String name, Category category, String ownerId, int deposit) {
        Validator.requireItemId(itemId);
        if (items.containsKey(itemId)) {
            throw new InvalidInputException("Item ID already exists: " + itemId);
        }
        getMember(ownerId);
        Item item = new Item(itemId, name, category, ownerId, deposit);
        items.put(itemId, item);
        return item;
    }

    // ---------------------------------------------------------------- FR3

    /**
     * FR3: borrows an item. The rules of BR4 are checked in a fixed order and the first
     * one that fails is reported as the reason of a {@link BorrowNotAllowedException}.
     */
    public Loan borrow(String memberId, String itemId, LocalDate startDate, int loanDays) {
        Validator.requireDate(startDate, "Start date");
        Validator.requireLoanDays(loanDays);
        Member member = getMember(memberId);
        Item item = getItem(itemId);
        TrustTier tier = member.getTier();

        if (item.isOwnedBy(memberId)) {
            throw new BorrowNotAllowedException(BorrowRefusal.OWN_ITEM,
                    memberId + " owns " + itemId);
        }
        if (!tier.canBorrow()) {
            throw new BorrowNotAllowedException(BorrowRefusal.LOW_TRUST,
                    "trust score " + member.getTrustScore() + " is below 30");
        }
        if (member.hasUnpaidFines()) {
            throw new BorrowNotAllowedException(BorrowRefusal.UNPAID_FINES,
                    member.getUnpaidFines() + " TL unpaid");
        }
        if (countActiveLoans(memberId) >= tier.getMaxActiveLoans()) {
            throw new BorrowNotAllowedException(BorrowRefusal.LOAN_LIMIT_REACHED,
                    tier + " members can have " + tier.getMaxActiveLoans() + " active loans");
        }
        if (loanDays > tier.getMaxLoanDays()) {
            throw new BorrowNotAllowedException(BorrowRefusal.LOAN_TOO_LONG,
                    tier + " members can borrow for at most " + tier.getMaxLoanDays() + " days");
        }
        if (!item.isAvailableFor(memberId)) {
            throw new BorrowNotAllowedException(BorrowRefusal.ITEM_NOT_AVAILABLE,
                    itemId + " is " + item.getStatus() + (item.isReserved() ? ", reserved" : ""));
        }

        int deposit = tier.depositFor(item.getDeposit());
        Loan loan = new Loan(item, member, startDate, loanDays, deposit);
        item.markBorrowed();
        if (memberId.equals(item.getReservedBy())) {
            item.clearReservation();
        }
        loans.add(loan);
        return loan;
    }

    // ---------------------------------------------------------------- FR4

    /** FR4: returns an item, settles the deposit and late fee, and updates the trust score. */
    public ReturnResult returnItem(String itemId, LocalDate returnDate, Condition condition) {
        Validator.requireDate(returnDate, "Return date");
        Validator.requireNotNull(condition, "Condition");
        Item item = getItem(itemId);
        Loan loan = getActiveLoan(item);

        int daysLate = loan.daysLateOn(returnDate);
        ReturnResult result = ReturnResult.settle(daysLate, loan.getDepositHeld(), condition);

        loan.close(returnDate);
        Member borrower = loan.getBorrower();
        borrower.addFine(result.getFineAdded());
        borrower.changeTrust(result.getTrustChange());
        item.markReturned(condition);
        return result;
    }

    // ---------------------------------------------------------------- FR5

    /**
     * FR5: renews a loan. The rules of BR6 are checked in a fixed order and the first
     * one that fails is reported as the reason of a {@link RenewalNotAllowedException}.
     */
    public Loan renew(String itemId, LocalDate renewalDate) {
        Validator.requireDate(renewalDate, "Renewal date");
        Item item = getItem(itemId);
        Loan loan = getActiveLoan(item);
        String borrowerId = loan.getBorrower().getId();

        if (renewalDate.isBefore(loan.getStartDate())) {
            throw new InvalidInputException(
                    "Renewal date " + renewalDate + " is before the start date " + loan.getStartDate());
        }
        if (loan.isOverdueOn(renewalDate)) {
            throw new RenewalNotAllowedException(RenewalRefusal.OVERDUE,
                    "due date was " + loan.getDueDate());
        }
        if (!loan.canBeRenewedAgain()) {
            throw new RenewalNotAllowedException(RenewalRefusal.RENEWAL_LIMIT_REACHED,
                    "a loan can be renewed " + Loan.MAX_RENEWALS + " time");
        }
        if (item.isReservedByOther(borrowerId)) {
            throw new RenewalNotAllowedException(RenewalRefusal.RESERVED_BY_OTHER,
                    itemId + " is reserved by " + item.getReservedBy());
        }

        loan.renew();
        return loan;
    }

    // ---------------------------------------------------------------- FR6, FR7, FR8

    /** FR6: pays part or all of a member's unpaid fines. */
    public void payFine(String memberId, int amount) {
        getMember(memberId).payFine(amount);
    }

    /** FR7: reserves a borrowed item for the member (BR7). */
    public void reserve(String memberId, String itemId) {
        getMember(memberId);
        Item item = getItem(itemId);
        if (item.getStatus() != ItemStatus.BORROWED) {
            throw new InvalidInputException("Only a borrowed item can be reserved; " + itemId + " is " + item.getStatus());
        }
        if (item.isReserved()) {
            throw new InvalidInputException(itemId + " is already reserved by " + item.getReservedBy());
        }
        if (item.isOwnedBy(memberId)) {
            throw new InvalidInputException("An owner cannot reserve their own item");
        }
        if (getActiveLoan(item).getBorrower().getId().equals(memberId)) {
            throw new InvalidInputException("The current borrower cannot reserve the item");
        }
        item.reserve(memberId);
    }

    /** FR8: the owner takes an item out of the catalogue. */
    public void withdrawItem(String memberId, String itemId) {
        requireOwner(memberId, itemId).withdraw();
    }

    /** FR8: the owner puts a withdrawn item back in the catalogue. */
    public void restoreItem(String memberId, String itemId) {
        requireOwner(memberId, itemId).restore();
    }

    // ---------------------------------------------------------------- FR9, FR10

    /** FR9: items in a category that the member could borrow now (not their own). */
    public List<Item> searchAvailable(String memberId, Category category) {
        getMember(memberId);
        Validator.requireNotNull(category, "Category");
        List<Item> result = new ArrayList<>();
        for (Item item : items.values()) {
            if (item.getCategory() == category
                    && !item.isOwnedBy(memberId)
                    && item.isAvailableFor(memberId)) {
                result.add(item);
            }
        }
        return result;
    }

    /** FR10: the member's loans that have not been returned. */
    public List<Loan> activeLoans(String memberId) {
        getMember(memberId);
        List<Loan> result = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.isActive() && loan.getBorrower().getId().equals(memberId)) {
                result.add(loan);
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- lookups

    public Member getMember(String memberId) {
        Validator.requireMemberId(memberId);
        Member member = members.get(memberId);
        if (member == null) {
            throw new NotFoundException("No member with ID " + memberId);
        }
        return member;
    }

    public Item getItem(String itemId) {
        Validator.requireItemId(itemId);
        Item item = items.get(itemId);
        if (item == null) {
            throw new NotFoundException("No item with ID " + itemId);
        }
        return item;
    }

    private int countActiveLoans(String memberId) {
        return activeLoans(memberId).size();
    }

    private Loan getActiveLoan(Item item) {
        for (Loan loan : loans) {
            if (loan.isActive() && loan.getItem() == item) {
                return loan;
            }
        }
        throw new InvalidInputException(item.getId() + " is not currently borrowed");
    }

    private Item requireOwner(String memberId, String itemId) {
        getMember(memberId);
        Item item = getItem(itemId);
        if (!item.isOwnedBy(memberId)) {
            throw new InvalidInputException(memberId + " does not own " + itemId);
        }
        return item;
    }
}
