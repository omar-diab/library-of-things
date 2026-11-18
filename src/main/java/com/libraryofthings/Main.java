package com.libraryofthings;

import com.libraryofthings.console.ConsoleApp;
import com.libraryofthings.service.LendingService;
import java.util.Scanner;

/** Starts the Library of Things console menu. */
public class Main {

    public static void main(String[] args) {
        new ConsoleApp(new LendingService(), new Scanner(System.in), System.out).run();
    }
}
