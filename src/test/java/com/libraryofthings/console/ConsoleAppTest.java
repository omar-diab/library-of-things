package com.libraryofthings.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.libraryofthings.model.ItemStatus;
import com.libraryofthings.service.LendingService;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsoleApp – menu driven by scripted input")
class ConsoleAppTest {

    private LendingService service;

    @BeforeEach
    void setUp() {
        service = new LendingService();
    }

    /** Runs the menu with the given lines as keyboard input and returns everything printed. */
    private String run(String... lines) {
        String input = String.join("\n", lines) + "\n";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        new ConsoleApp(service, new Scanner(input), out).run();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Register a member and show it")
    void registerAndShow() {
        String output = run("1", "M0001", "Omar", "12", "M0001", "0");
        assertTrue(output.contains("Registered M0001 Omar (trust 50, STANDARD, unpaid fines 0 TL)"));
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    @DisplayName("Full flow: sample data, borrow, return late, see the result")
    void borrowAndReturnFlow() {
        String output = run(
                "13",
                "3", "M0002", "I0001", "2026-11-02", "7",
                "4", "I0001", "2026-11-17", "good",
                "0");
        assertTrue(output.contains("Borrowed Cordless drill. Due on 2026-11-09. Deposit held: 200 TL"));
        assertTrue(output.contains("Days late: 8, late fee: 45 TL, deposit refunded: 155 TL"));
        assertEquals(30, service.getMember("M0002").getTrustScore());
        assertEquals(ItemStatus.AVAILABLE, service.getItem("I0001").getStatus());
    }

    @Test
    @DisplayName("A refused borrow shows the reason and the menu keeps running")
    void refusalShowsReason() {
        String output = run("13", "3", "M0001", "I0001", "2026-11-02", "7", "12", "M0001", "0");
        assertTrue(output.contains("Error: OWN_ITEM"));
        assertTrue(output.contains("M0001 Omar (trust 50"));
    }

    @Test
    @DisplayName("Negative: bad number, bad date and bad category are reported, not crashed")
    void badInputIsReported() {
        // An invalid answer stops the current action at once and goes back to the menu,
        // so no further answers are given for that action.
        String output = run(
                "13",
                "3", "M0002", "I0001", "2026-11-02", "seven",
                "3", "M0002", "I0001", "02/11/2026",
                "10", "M0002", "garden",
                "99",
                "0");
        assertTrue(output.contains("Error: Not a whole number: seven"));
        assertTrue(output.contains("Error: Not a valid date (use YYYY-MM-DD): 02/11/2026"));
        assertTrue(output.contains("Error: Unknown value: garden"));
        assertTrue(output.contains("Unknown option: 99"));
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    @DisplayName("Input ending in the middle of a question exits cleanly")
    void inputEndsEarly() {
        String output = run("1", "M0001");
        assertTrue(output.endsWith("Goodbye!" + System.lineSeparator()));
    }

    @Test
    @DisplayName("Search lists items, and says so when there are none")
    void search() {
        String output = run("13", "10", "M0002", "tools", "10", "M0002", "kitchen", "0");
        assertTrue(output.contains(" - I0001 Cordless drill [TOOLS]"));
        assertTrue(output.contains(" - I0002 Ladder [TOOLS]"));
        assertTrue(output.contains("No available items in this category."));
    }
}
