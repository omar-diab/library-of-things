package com.libraryofthings;

public class App {
    public static String greet(String name) {
        return "Welcome to Library of Things, " + name + "!";
    }

    public static void main(String[] args) {
        System.out.println(greet("Omar"));
    }
}
