package com.libraryofthings.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.libraryofthings.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Item")
class ItemTest {

    private Item drill;

    @BeforeEach
    void setUp() {
        drill = new Item("I0001", "Drill", Category.TOOLS, "M0001", 200);
    }

    @Nested
    @DisplayName("Adding an item (FR2)")
    class Creation {

        @Test
        @DisplayName("A new item is AVAILABLE and not reserved")
        void newItemDefaults() {
            assertEquals("I0001", drill.getId());
            assertEquals(Category.TOOLS, drill.getCategory());
            assertEquals(200, drill.getDeposit());
            assertEquals(ItemStatus.AVAILABLE, drill.getStatus());
            assertFalse(drill.isReserved());
            assertNull(drill.getReservedBy());
            assertTrue(drill.isOwnedBy("M0001"));
            assertFalse(drill.isOwnedBy("M0002"));
        }

        @ParameterizedTest(name = "deposit {0} TL is accepted")
        @DisplayName("BVA: deposits at the edges of 0-1000 are accepted")
        @ValueSource(ints = {0, 1, 999, 1000})
        void validDeposit(int deposit) {
            Item item = new Item("I0002", "Tent", Category.OUTDOOR, "M0001", deposit);
            assertEquals(deposit, item.getDeposit());
        }

        @ParameterizedTest(name = "deposit {0} TL is rejected")
        @DisplayName("BVA / negative: deposits just outside 0-1000 are rejected")
        @ValueSource(ints = {-1, 1001})
        void invalidDeposit(int deposit) {
            assertThrows(InvalidInputException.class,
                    () -> new Item("I0002", "Tent", Category.OUTDOOR, "M0001", deposit));
        }

        @Test
        @DisplayName("Negative: a missing category is rejected")
        void nullCategoryIsRejected() {
            assertThrows(InvalidInputException.class,
                    () -> new Item("I0002", "Tent", null, "M0001", 100));
        }

        @Test
        @DisplayName("Negative: an invalid owner ID is rejected")
        void invalidOwnerIsRejected() {
            assertThrows(InvalidInputException.class,
                    () -> new Item("I0002", "Tent", Category.OUTDOOR, "owner", 100));
        }
    }

    @Nested
    @DisplayName("Withdraw and restore (FR8)")
    class WithdrawRestore {

        @Test
        @DisplayName("An available item can be withdrawn and restored")
        void withdrawThenRestore() {
            drill.withdraw();
            assertEquals(ItemStatus.WITHDRAWN, drill.getStatus());
            drill.restore();
            assertEquals(ItemStatus.AVAILABLE, drill.getStatus());
        }

        @Test
        @DisplayName("Negative: a borrowed item cannot be withdrawn")
        void borrowedItemCannotBeWithdrawn() {
            drill.markBorrowed();
            assertThrows(InvalidInputException.class, drill::withdraw);
            assertEquals(ItemStatus.BORROWED, drill.getStatus());
        }

        @Test
        @DisplayName("Negative: an available item cannot be restored")
        void availableItemCannotBeRestored() {
            assertThrows(InvalidInputException.class, drill::restore);
        }
    }

    @Nested
    @DisplayName("Availability and reservations")
    class Availability {

        @Test
        @DisplayName("An available, unreserved item is available to anyone")
        void availableToAnyone() {
            assertTrue(drill.isAvailableFor("M0002"));
        }

        @Test
        @DisplayName("A reserved item is available only to the member who reserved it")
        void reservedItemOnlyForReserver() {
            drill.reserve("M0002");
            assertTrue(drill.isAvailableFor("M0002"));
            assertFalse(drill.isAvailableFor("M0003"));
            assertTrue(drill.isReservedByOther("M0003"));
            assertFalse(drill.isReservedByOther("M0002"));
        }

        @Test
        @DisplayName("Borrowed and withdrawn items are not available")
        void borrowedOrWithdrawnNotAvailable() {
            drill.markBorrowed();
            assertFalse(drill.isAvailableFor("M0002"));
            drill.markReturned(Condition.DAMAGED);
            assertEquals(ItemStatus.WITHDRAWN, drill.getStatus());
            assertFalse(drill.isAvailableFor("M0002"));
        }

        @Test
        @DisplayName("Returned in good condition makes the item available again")
        void returnedGoodIsAvailable() {
            drill.markBorrowed();
            drill.markReturned(Condition.GOOD);
            assertEquals(ItemStatus.AVAILABLE, drill.getStatus());
        }
    }
}
