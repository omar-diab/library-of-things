package com.libraryofthings.exception;

/**
 * Thrown when an input value breaks a validation rule (BR8),
 * for example a badly formatted ID or a negative deposit.
 */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
