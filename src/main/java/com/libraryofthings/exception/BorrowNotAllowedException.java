package com.libraryofthings.exception;

/** Thrown when a borrow request breaks a business rule (BR4). The reason says which rule. */
public class BorrowNotAllowedException extends RuntimeException {

    private final BorrowRefusal reason;

    public BorrowNotAllowedException(BorrowRefusal reason, String message) {
        super(reason + ": " + message);
        this.reason = reason;
    }

    public BorrowRefusal getReason() {
        return reason;
    }
}
