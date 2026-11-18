package com.libraryofthings.console;

import com.libraryofthings.exception.BorrowNotAllowedException;
import com.libraryofthings.exception.InvalidInputException;
import com.libraryofthings.exception.NotFoundException;
import com.libraryofthings.exception.RenewalNotAllowedException;
import com.libraryofthings.model.Category;
import com.libraryofthings.model.Condition;
import com.libraryofthings.model.Item;
import com.libraryofthings.model.Loan;
import com.libraryofthings.model.Member;
import com.libraryofthings.rules.ReturnResult;
import com.libraryofthings.service.LendingService;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * A text menu for trying the lending engine by hand.
 * Input and output are passed in, so the menu can also be tested automatically.
 */
public class ConsoleApp {

    private static final String MENU = String.join(System.lineSeparator(),
            "",
            "=== Library of Things ===",
            " 1. Register member        2. Add item",
            " 3. Borrow item            4. Return item",
            " 5. Renew loan             6. Reserve item",
            " 7. Pay fine               8. Withdraw item",
            " 9. Restore item          10. Search available items",
            "11. Show member's loans   12. Show member",
            "13. Load sample data       0. Exit");

    private final LendingService service;
    private final Scanner in;
    private final PrintStream out;

    public ConsoleApp(LendingService service, Scanner in, PrintStream out) {
        this.service = service;
        this.in = in;
        this.out = out;
    }

    /** Shows the menu until the user chooses 0 or the input ends. */
    public void run() {
        while (true) {
            out.println(MENU);
            String choice = ask("Choose");
            if (choice == null || choice.equals("0")) {
                out.println("Goodbye!");
                return;
            }
            try {
                handle(choice);
            } catch (InvalidInputException | NotFoundException
                     | BorrowNotAllowedException | RenewalNotAllowedException e) {
                out.println("Error: " + e.getMessage());
            } catch (InputEndedException e) {
                out.println("Goodbye!");
                return;
            }
        }
    }

    private void handle(String choice) {
        switch (choice) {
            case "1" -> registerMember();
            case "2" -> addItem();
            case "3" -> borrow();
            case "4" -> returnItem();
            case "5" -> renew();
            case "6" -> reserve();
            case "7" -> payFine();
            case "8" -> withdraw();
            case "9" -> restore();
            case "10" -> search();
            case "11" -> showLoans();
            case "12" -> showMember();
            case "13" -> loadSampleData();
            default -> out.println("Unknown option: " + choice);
        }
    }

    // ---------------------------------------------------------------- actions

    private void registerMember() {
        Member member = service.registerMember(askRequired("Member ID (e.g. M0001)"), askRequired("Name"));
        out.println("Registered " + describe(member));
    }

    private void addItem() {
        Item item = service.addItem(
                askRequired("Item ID (e.g. I0001)"),
                askRequired("Item name"),
                askCategory(),
                askRequired("Owner member ID"),
                askInt("Deposit in TL (0-1000)"));
        out.println("Added " + describe(item));
    }

    private void borrow() {
        Loan loan = service.borrow(
                askRequired("Member ID"),
                askRequired("Item ID"),
                askDate("Start date (YYYY-MM-DD)"),
                askInt("Loan length in days"));
        out.println("Borrowed " + loan.getItem().getName() + ". Due on " + loan.getDueDate()
                + ". Deposit held: " + loan.getDepositHeld() + " TL");
    }

    private void returnItem() {
        String itemId = askRequired("Item ID");
        LocalDate date = askDate("Return date (YYYY-MM-DD)");
        Condition condition = askEnum(Condition.class, "Condition (GOOD/DAMAGED)");
        ReturnResult result = service.returnItem(itemId, date, condition);
        out.println("Returned. Days late: " + result.getDaysLate()
                + ", late fee: " + result.getLateFee() + " TL"
                + ", deposit refunded: " + result.getDepositRefunded() + " TL"
                + ", paid to owner: " + result.getDepositToOwner() + " TL"
                + ", fine added: " + result.getFineAdded() + " TL"
                + ", trust change: " + result.getTrustChange()
                + ", owner trust change: " + result.getOwnerTrustChange());
    }

    private void renew() {
        Loan loan = service.renew(askRequired("Item ID"), askDate("Renewal date (YYYY-MM-DD)"));
        out.println("Renewed. New due date: " + loan.getDueDate());
    }

    private void reserve() {
        String memberId = askRequired("Member ID");
        String itemId = askRequired("Item ID");
        service.reserve(memberId, itemId);
        out.println("Reserved " + itemId + " for " + memberId);
    }

    private void payFine() {
        String memberId = askRequired("Member ID");
        service.payFine(memberId, askInt("Amount in TL"));
        out.println("Paid. Unpaid fines now: " + service.getMember(memberId).getUnpaidFines() + " TL");
    }

    private void withdraw() {
        String memberId = askRequired("Owner member ID");
        String itemId = askRequired("Item ID");
        service.withdrawItem(memberId, itemId);
        out.println("Withdrew " + itemId);
    }

    private void restore() {
        String memberId = askRequired("Owner member ID");
        String itemId = askRequired("Item ID");
        service.restoreItem(memberId, itemId);
        out.println("Restored " + itemId);
    }

    private void search() {
        List<Item> found = service.searchAvailable(askRequired("Member ID"), askCategory());
        if (found.isEmpty()) {
            out.println("No available items in this category.");
        }
        for (Item item : found) {
            out.println(" - " + describe(item));
        }
    }

    private void showLoans() {
        List<Loan> loans = service.activeLoans(askRequired("Member ID"));
        if (loans.isEmpty()) {
            out.println("No active loans.");
        }
        for (Loan loan : loans) {
            out.println(" - " + loan.getItem().getId() + " " + loan.getItem().getName()
                    + ", due " + loan.getDueDate() + ", renewals " + loan.getRenewals());
        }
    }

    private void showMember() {
        out.println(describe(service.getMember(askRequired("Member ID"))));
    }

    private void loadSampleData() {
        service.registerMember("M0001", "Omar");
        service.registerMember("M0002", "Sara");
        service.registerMember("M0003", "Ali");
        service.addItem("I0001", "Cordless drill", Category.TOOLS, "M0001", 200);
        service.addItem("I0002", "Ladder", Category.TOOLS, "M0001", 150);
        service.addItem("I0003", "Camping tent", Category.OUTDOOR, "M0002", 300);
        service.addItem("I0004", "Projector", Category.ELECTRONICS, "M0003", 500);
        service.addItem("I0005", "Stand mixer", Category.KITCHEN, "M0002", 100);
        out.println("Loaded 3 members (M0001-M0003) and 5 items (I0001-I0005).");
    }

    // ---------------------------------------------------------------- input helpers

    private String ask(String prompt) {
        out.print(prompt + ": ");
        if (!in.hasNextLine()) {
            return null;
        }
        return in.nextLine().trim();
    }

    private String askRequired(String prompt) {
        String answer = ask(prompt);
        if (answer == null) {
            throw new InputEndedException();
        }
        return answer;
    }

    private int askInt(String prompt) {
        String answer = askRequired(prompt);
        try {
            return Integer.parseInt(answer);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Not a whole number: " + answer);
        }
    }

    private LocalDate askDate(String prompt) {
        String answer = askRequired(prompt);
        try {
            return LocalDate.parse(answer);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Not a valid date (use YYYY-MM-DD): " + answer);
        }
    }

    private Category askCategory() {
        return askEnum(Category.class, "Category (TOOLS/OUTDOOR/ELECTRONICS/KITCHEN/OTHER)");
    }

    private <E extends Enum<E>> E askEnum(Class<E> type, String prompt) {
        String answer = askRequired(prompt);
        try {
            return Enum.valueOf(type, answer.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Unknown value: " + answer);
        }
    }

    private static String describe(Member member) {
        return member.getId() + " " + member.getName()
                + " (trust " + member.getTrustScore() + ", " + member.getTier()
                + ", unpaid fines " + member.getUnpaidFines() + " TL)";
    }

    private static String describe(Item item) {
        return item.getId() + " " + item.getName() + " [" + item.getCategory() + "]"
                + ", owner " + item.getOwnerId() + ", deposit " + item.getDeposit() + " TL"
                + ", " + item.getStatus()
                + (item.isReserved() ? ", reserved by " + item.getReservedBy() : "");
    }

    /** Signals that the input ended in the middle of a question. */
    private static class InputEndedException extends RuntimeException {
    }
}
