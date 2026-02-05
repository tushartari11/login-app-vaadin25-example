package com.example.vaadin.services;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Example service demonstrating CDI injection in Vaadin.
 */
@ApplicationScoped
public class GreetingService {

    public String getGreeting(String name) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return String.format("Hello, %s! Welcome at %s", name, timestamp);
    }

    public String getServerInfo() {
        return "Running on Payara 6 with Java " + System.getProperty("java.version");
    }
}
