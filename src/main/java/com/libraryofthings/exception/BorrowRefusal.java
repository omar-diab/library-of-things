package com.libraryofthings.exception;

/** Why a borrow request was refused (BR4). Listed in the order the rules are checked. */
public enum BorrowRefusal {
    OWN_ITEM,
    LOW_TRUST,
    UNPAID_FINES,
    LOAN_LIMIT_REACHED,
    LOAN_TOO_LONG,
    ITEM_NOT_AVAILABLE
}
