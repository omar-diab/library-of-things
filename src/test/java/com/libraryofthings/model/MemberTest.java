package com.libraryofthings.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.libraryofthings.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Member")
class MemberTest {

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member("M0001", "Omar");
    }

    @Nested
    @DisplayName("Registration (FR1)")
    class Registration {

        @Test
        @DisplayName("A new member starts with 50 trust, STANDARD tier and no fines")
        void newMemberDefaults() {
            assertEquals("M0001", member.getId());
            assertEquals("Omar", member.getName());
            assertEquals(50, member.getTrustScore());
            assertEquals(TrustTier.STANDARD, member.getTier());
            assertEquals(0, member.getUnpaidFines());
            assertFalse(member.hasUnpaidFines());
        }

        @ParameterizedTest(name = "invalid name \"{0}\"")
        @DisplayName("Negative: an empty name is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void invalidNameIsRejected(String name) {
            assertThrows(InvalidInputException.class, () -> new Member("M0002", name));
        }

        @Test
        @DisplayName("Negative: an invalid ID is rejected")
        void invalidIdIsRejected() {
            assertThrows(InvalidInputException.class, () -> new Member("X0002", "Ali"));
        }
    }

    @Nested
    @DisplayName("Trust score (BR2)")
    class TrustScore {

        @ParameterizedTest(name = "50 {0} -> {1}")
        @DisplayName("BVA: the score is kept between 0 and 100")
        @CsvSource({
            "+49, 99",
            "+50, 100",
            "+51, 100",
            "-49, 1",
            "-50, 0",
            "-51, 0"
        })
        void scoreIsClamped(int delta, int expected) {
            member.changeTrust(delta);
            assertEquals(expected, member.getTrustScore());
        }

        @Test
        @DisplayName("A member at 100 stays at 100 after +5")
        void cannotGoAboveMaximum() {
            member.changeTrust(50);
            member.changeTrust(5);
            assertEquals(100, member.getTrustScore());
        }

        @Test
        @DisplayName("Tier changes when the score crosses a boundary")
        void tierFollowsScore() {
            member.changeTrust(-21); // 29
            assertEquals(TrustTier.BLOCKED, member.getTier());
            member.changeTrust(1); // 30
            assertEquals(TrustTier.STANDARD, member.getTier());
            member.changeTrust(39); // 69
            assertEquals(TrustTier.STANDARD, member.getTier());
            member.changeTrust(1); // 70
            assertEquals(TrustTier.TRUSTED, member.getTier());
        }
    }

    @Nested
    @DisplayName("Fines (FR6)")
    class Fines {

        @BeforeEach
        void addFine() {
            member.addFine(30);
        }

        @Test
        @DisplayName("Adding a fine increases the unpaid balance")
        void addFineIncreasesBalance() {
            assertEquals(30, member.getUnpaidFines());
            assertTrue(member.hasUnpaidFines());
        }

        @ParameterizedTest(name = "pay {0} of 30 -> {1} left")
        @DisplayName("BVA: valid payments from 1 TL up to the full amount")
        @CsvSource({
            "1,  29",
            "29, 1",
            "30, 0"
        })
        void validPayment(int amount, int remaining) {
            member.payFine(amount);
            assertEquals(remaining, member.getUnpaidFines());
        }

        @ParameterizedTest(name = "paying {0} is rejected")
        @DisplayName("BVA / negative: payments of 0, below 0 or above the balance are rejected")
        @ValueSource(ints = {0, -5, 31})
        void invalidPayment(int amount) {
            assertThrows(InvalidInputException.class, () -> member.payFine(amount));
            assertEquals(30, member.getUnpaidFines());
        }

        @Test
        @DisplayName("Negative: a negative fine cannot be added")
        void negativeFineIsRejected() {
            assertThrows(InvalidInputException.class, () -> member.addFine(-1));
        }
    }
}
