package com.libraryofthings.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.exception.NotFoundException;
import com.libraryofthings.model.Category;
import com.libraryofthings.model.Condition;
import com.libraryofthings.model.Item;
import com.libraryofthings.model.ItemStatus;
import com.libraryofthings.model.Loan;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LendingService – members, items, reservations and queries")
class LendingServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 2);
    private static final String OWNER = "M0001";
    private static final String BORROWER = "M0002";
    private static final String OTHER = "M0003";

    private LendingService service;

    @BeforeEach
    void setUp() {
        service = new LendingService();
        service.registerMember(OWNER, "Owner");
        service.registerMember(BORROWER, "Borrower");
        service.registerMember(OTHER, "Other");
        service.addItem("I0001", "Drill", Category.TOOLS, OWNER, 200);
        service.addItem("I0002", "Ladder", Category.TOOLS, OWNER, 150);
        service.addItem("I0003", "Tent", Category.OUTDOOR, OWNER, 300);
        service.addItem("I0004", "Saw", Category.TOOLS, BORROWER, 100);
    }

    @Nested
    @DisplayName("Registering members (FR1)")
    class Members {

        @Test
        @DisplayName("Negative: a duplicate member ID is rejected")
        void duplicateMember() {
            assertThrows(InvalidInputException.class, () -> service.registerMember(OWNER, "Copy"));
        }

        @Test
        @DisplayName("Negative: an unknown member is not found")
        void unknownMember() {
            assertThrows(NotFoundException.class, () -> service.getMember("M9999"));
        }
    }

    @Nested
    @DisplayName("Adding items (FR2)")
    class Items {

        @Test
        @DisplayName("Negative: a duplicate item ID is rejected")
        void duplicateItem() {
            assertThrows(InvalidInputException.class,
                    () -> service.addItem("I0001", "Copy", Category.TOOLS, OWNER, 10));
        }

        @Test
        @DisplayName("Negative: an item needs a registered owner")
        void unknownOwner() {
            assertThrows(NotFoundException.class,
                    () -> service.addItem("I0009", "Kettle", Category.KITCHEN, "M9999", 10));
        }

        @Test
        @DisplayName("Negative: an unknown item is not found")
        void unknownItem() {
            assertThrows(NotFoundException.class, () -> service.getItem("I9999"));
        }
    }

    @Nested
    @DisplayName("Paying fines (FR6)")
    class Fines {

        @Test
        @DisplayName("Paying through the service reduces the member's fines")
        void payFine() {
            service.getMember(BORROWER).addFine(40);
            service.payFine(BORROWER, 15);
            assertEquals(25, service.getMember(BORROWER).getUnpaidFines());
        }

        @Test
        @DisplayName("Negative: paying more than owed is rejected")
        void overpay() {
            assertThrows(InvalidInputException.class, () -> service.payFine(BORROWER, 1));
        }
    }

    @Nested
    @DisplayName("Reservations (FR7, BR7)")
    class Reservations {

        @Test
        @DisplayName("Another member can reserve a borrowed item")
        void reserveBorrowedItem() {
            service.borrow(BORROWER, "I0001", START, 7);
            service.reserve(OTHER, "I0001");
            assertEquals(OTHER, service.getItem("I0001").getReservedBy());
        }

        @Test
        @DisplayName("Negative: an available item cannot be reserved")
        void availableItem() {
            assertThrows(InvalidInputException.class, () -> service.reserve(OTHER, "I0001"));
        }

        @Test
        @DisplayName("Negative: an item can only have one reservation")
        void alreadyReserved() {
            service.borrow(BORROWER, "I0001", START, 7);
            service.reserve(OTHER, "I0001");
            service.registerMember("M0004", "Fourth");
            assertThrows(InvalidInputException.class, () -> service.reserve("M0004", "I0001"));
        }

        @Test
        @DisplayName("Negative: the current borrower cannot reserve the item")
        void borrowerCannotReserve() {
            service.borrow(BORROWER, "I0001", START, 7);
            assertThrows(InvalidInputException.class, () -> service.reserve(BORROWER, "I0001"));
        }

        @Test
        @DisplayName("Negative: the owner cannot reserve their own item")
        void ownerCannotReserve() {
            service.borrow(BORROWER, "I0001", START, 7);
            assertThrows(InvalidInputException.class, () -> service.reserve(OWNER, "I0001"));
        }
    }

    @Nested
    @DisplayName("Withdrawing and restoring (FR8)")
    class WithdrawRestore {

        @Test
        @DisplayName("The owner can withdraw and restore an available item")
        void ownerWithdrawsAndRestores() {
            service.withdrawItem(OWNER, "I0001");
            assertEquals(ItemStatus.WITHDRAWN, service.getItem("I0001").getStatus());
            service.restoreItem(OWNER, "I0001");
            assertEquals(ItemStatus.AVAILABLE, service.getItem("I0001").getStatus());
        }

        @Test
        @DisplayName("Negative: only the owner can withdraw or restore")
        void notOwner() {
            assertThrows(InvalidInputException.class, () -> service.withdrawItem(BORROWER, "I0001"));
            service.withdrawItem(OWNER, "I0001");
            assertThrows(InvalidInputException.class, () -> service.restoreItem(BORROWER, "I0001"));
        }

        @Test
        @DisplayName("Negative: a borrowed item cannot be withdrawn")
        void borrowedItem() {
            service.borrow(BORROWER, "I0001", START, 7);
            assertThrows(InvalidInputException.class, () -> service.withdrawItem(OWNER, "I0001"));
        }

        @Test
        @DisplayName("The owner restores an item that came back damaged")
        void restoreAfterDamage() {
            service.borrow(BORROWER, "I0001", START, 7);
            service.returnItem("I0001", START.plusDays(7), Condition.DAMAGED);
            service.restoreItem(OWNER, "I0001");
            assertEquals(ItemStatus.AVAILABLE, service.getItem("I0001").getStatus());
        }
    }

    @Nested
    @DisplayName("Searching and listing (FR9, FR10)")
    class Queries {

        @Test
        @DisplayName("Search returns available items in the category, not the member's own")
        void searchAvailable() {
            List<Item> tools = service.searchAvailable(BORROWER, Category.TOOLS);
            assertEquals(List.of(service.getItem("I0001"), service.getItem("I0002")), tools);
        }

        @Test
        @DisplayName("Search skips borrowed and withdrawn items")
        void searchSkipsUnavailable() {
            service.borrow(OTHER, "I0001", START, 7);
            service.withdrawItem(OWNER, "I0002");
            assertTrue(service.searchAvailable(BORROWER, Category.TOOLS).isEmpty());
        }

        @Test
        @DisplayName("Search shows a reserved item only to the member who reserved it")
        void searchWithReservation() {
            service.borrow(OTHER, "I0003", START, 7);
            service.reserve(BORROWER, "I0003");
            service.returnItem("I0003", START.plusDays(2), Condition.GOOD);

            assertEquals(1, service.searchAvailable(BORROWER, Category.OUTDOOR).size());
            service.registerMember("M0004", "Fourth");
            assertTrue(service.searchAvailable("M0004", Category.OUTDOOR).isEmpty());
        }

        @Test
        @DisplayName("Negative: search needs a category")
        void searchWithoutCategory() {
            assertThrows(InvalidInputException.class, () -> service.searchAvailable(BORROWER, null));
        }

        @Test
        @DisplayName("Active loans list only loans that are not returned")
        void activeLoans() {
            Loan drill = service.borrow(BORROWER, "I0001", START, 7);
            service.borrow(BORROWER, "I0002", START, 7);
            service.returnItem("I0002", START.plusDays(3), Condition.GOOD);

            assertEquals(List.of(drill), service.activeLoans(BORROWER));
            assertTrue(service.activeLoans(OTHER).isEmpty());
        }
    }
}
