package com.libraryofthings.model;

import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.validation.Validator;

/**
 * An item a member offers for lending (FR2).
 * A new item is AVAILABLE and not reserved.
 */
public class Item {

    private final String id;
    private final String name;
    private final Category category;
    private final String ownerId;
    private final int deposit;
    private ItemStatus status;
    private String reservedBy;

    public Item(String id, String name, Category category, String ownerId, int deposit) {
        Validator.requireItemId(id);
        Validator.requireName(name, "Item name");
        Validator.requireNotNull(category, "Category");
        Validator.requireMemberId(ownerId);
        Validator.requireDeposit(deposit);
        this.id = id;
        this.name = name;
        this.category = category;
        this.ownerId = ownerId;
        this.deposit = deposit;
        this.status = ItemStatus.AVAILABLE;
        this.reservedBy = null;
    }

    public boolean isOwnedBy(String memberId) {
        return ownerId.equals(memberId);
    }

    public boolean isReserved() {
        return reservedBy != null;
    }

    /** True when someone other than the given member holds the reservation. */
    public boolean isReservedByOther(String memberId) {
        return reservedBy != null && !reservedBy.equals(memberId);
    }

    /** True when the given member may take the item now (part of BR4, check 6). */
    public boolean isAvailableFor(String memberId) {
        return status == ItemStatus.AVAILABLE && !isReservedByOther(memberId);
    }

    /** Owner takes the item out of the catalogue (FR8). */
    public void withdraw() {
        if (status != ItemStatus.AVAILABLE) {
            throw new InvalidInputException("Only an available item can be withdrawn; status is " + status);
        }
        status = ItemStatus.WITHDRAWN;
    }

    /** Owner puts a withdrawn item back in the catalogue (FR8). */
    public void restore() {
        if (status != ItemStatus.WITHDRAWN) {
            throw new InvalidInputException("Only a withdrawn item can be restored; status is " + status);
        }
        status = ItemStatus.AVAILABLE;
    }

    public void markBorrowed() {
        status = ItemStatus.BORROWED;
    }

    public void markReturned(Condition condition) {
        status = (condition == Condition.DAMAGED) ? ItemStatus.WITHDRAWN : ItemStatus.AVAILABLE;
    }

    public void reserve(String memberId) {
        reservedBy = memberId;
    }

    public void clearReservation() {
        reservedBy = null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public int getDeposit() {
        return deposit;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public String getReservedBy() {
        return reservedBy;
    }
}
