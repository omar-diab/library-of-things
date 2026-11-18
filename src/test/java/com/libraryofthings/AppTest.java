package com.libraryofthings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AppTest {
    @Test
    void greetReturnsWelcomeMessage() {
        assertEquals("Welcome to Library of Things, Omar!", App.greet("Omar"));
    }
}
