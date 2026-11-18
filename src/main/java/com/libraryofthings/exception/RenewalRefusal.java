package com.libraryofthings.exception;

/** Why a renewal request was refused (BR6). Listed in the order the rules are checked. */
public enum RenewalRefusal {
    OVERDUE,
    RENEWAL_LIMIT_REACHED,
    RESERVED_BY_OTHER
}
