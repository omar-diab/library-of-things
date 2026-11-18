package com.libraryofthings.exception;

/** Thrown when a renewal request breaks a business rule (BR6). The reason says which rule. */
public class RenewalNotAllowedException extends RuntimeException {

    private final RenewalRefusal reason;

    public RenewalNotAllowedException(RenewalRefusal reason, String message) {
        super(reason + ": " + message);
        this.reason = reason;
    }

    public RenewalRefusal getReason() {
        return reason;
    }
}
