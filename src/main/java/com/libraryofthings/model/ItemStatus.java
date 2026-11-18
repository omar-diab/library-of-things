package com.libraryofthings.model;

/** Where an item is in its lending life cycle. */
public enum ItemStatus {
    /** Can be borrowed (unless reserved by someone else). */
    AVAILABLE,
    /** Currently with a borrower. */
    BORROWED,
    /** Taken out of the catalogue by its owner (or after being returned damaged). */
    WITHDRAWN
}
