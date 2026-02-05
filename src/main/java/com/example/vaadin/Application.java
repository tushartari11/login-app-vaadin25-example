package com.example.vaadin;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Application shell configuration for Vaadin application.
 * This class is automatically discovered by Vaadin.
 */
@ApplicationScoped
@Theme("my-theme")
public class Application implements AppShellConfigurator {
    
}
