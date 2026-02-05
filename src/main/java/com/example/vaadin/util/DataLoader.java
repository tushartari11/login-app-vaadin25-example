package com.example.vaadin.util;

import com.example.vaadin.security.Role;
import com.example.vaadin.services.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads initial test data on application startup.
 * FOR DEVELOPMENT ONLY - Remove in production.
 */
@Singleton
@Startup
public class DataLoader {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    @Inject
    private UserService userService;

    @PostConstruct
    public void loadData() {
        if (userService.getUserCount() == 0) {
            logger.info("Loading test data...");

            try {
                userService.registerUser(
                    "admin",
                    "admin@example.com",
                    "Admin@123",
                    Role.ADMIN
                );

                userService.registerUser(
                    "manager",
                    "manager@example.com",
                    "Manager@123",
                    Role.MANAGER
                );

                userService.registerUser(
                    "user",
                    "user@example.com",
                    "User@123",
                    Role.USER
                );

                userService.registerUser(
                    "guest",
                    "guest@example.com",
                    "Guest@123",
                    Role.GUEST
                );

                logger.info("Test data loaded successfully");
            } catch (Exception e) {
                logger.error("Failed to load test data", e);
            }
        }
    }
}
