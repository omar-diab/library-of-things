package com.libraryofthings.exception;

/** Thrown when a member ID or item ID is well formed but not registered. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
