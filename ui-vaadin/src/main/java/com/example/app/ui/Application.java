package com.example.app.ui;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Application shell configuration for Vaadin application. This class is automatically discovered by
 * Vaadin. Vaadin 25 ships the LumoUtility classes as a separate stylesheet; without it every
 * LumoUtility class name used in the views has no effect.
 */
@ApplicationScoped
@Theme("my-theme")
@StyleSheet(Lumo.UTILITY_STYLESHEET)
public class Application implements AppShellConfigurator {

}
