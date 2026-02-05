package com.example.vaadin.views;

import com.example.vaadin.application.theme.ThemeSwitcher;
import com.example.vaadin.security.SecurityService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.inject.Inject;

/**
 * Main application layout with header, content area, and footer.
 * WCAG 2.1 AAA compliant with proper contrast ratios.
 * Styles are defined in app-layout.css
 */
@StyleSheet("./styles/app-layout.css")
@StyleSheet("./styles/notifications.css")
public class AppLayout extends VerticalLayout {

    private final SecurityService securityService;
    private final VerticalLayout contentArea;

    @Inject
    public AppLayout(SecurityService securityService) {
        this.securityService = securityService;
        this.contentArea = new VerticalLayout();

        setupRootLayout();
        createHeader();
        setupContentArea();
        createFooter();
    }

    private void setupRootLayout() {
        setSpacing(false);
        setPadding(false);
        setMargin(false);
        setSizeFull();
        addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                "app-layout");
    }

    /**
     * Creates the application header with logo, title, theme selector, and logout
     * button.
     */
    private void createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.addClassName("app-header");

        // Left side: Logo and company name
        HorizontalLayout logoSection = createLogoSection();

        // Right side: Theme selector and logout button
        HorizontalLayout actionSection = createActionSection();

        header.add(logoSection, actionSection);
        add(header);
    }

    /**
     * Creates the logo section with company logo and name.
     */
    private HorizontalLayout createLogoSection() {
        HorizontalLayout logoSection = new HorizontalLayout();
        logoSection.setSpacing(true);
        logoSection.setAlignItems(FlexComponent.Alignment.CENTER);
        logoSection.addClassName("app-header__logo-section");

        // Company logo
        Image logo = new Image("./images/Logo_orig_bg.jpg", "Company Logo");
        logo.setWidth("48px");
        logo.setHeight("48px");
        logo.addClassName("app-header__logo");

        H1 companyName = new H1("Vaadin App");
        companyName.addClassName("app-header__company-name");

        logoSection.add(logo, companyName);
        return logoSection;
    }

    /**
     * Creates the action section with theme selector and logout button.
     */
    private HorizontalLayout createActionSection() {
        HorizontalLayout actionSection = new HorizontalLayout();
        actionSection.setSpacing(true);
        actionSection.setAlignItems(FlexComponent.Alignment.CENTER);
        actionSection.addClassName("app-header__actions");

        // Theme selector
        RadioButtonGroup<ThemeSwitcher.Theme> themeGroup = new RadioButtonGroup<>();
        themeGroup.setItems(ThemeSwitcher.Theme.values());
        themeGroup.setItemLabelGenerator(ThemeSwitcher.Theme::getDisplayName);
        themeGroup.setValue(ThemeSwitcher.getCurrentTheme());
        themeGroup.setLabel("Theme");
        themeGroup.addClassName("app-header__theme-selector");
        themeGroup.addValueChangeListener(event -> {
            ThemeSwitcher.switchTheme(event.getValue());
            Notification.show("Theme changed to " + event.getValue().getDisplayName(), 2000,
                    Notification.Position.TOP_CENTER);
        });

        // Logout button
        Button logoutButton = new Button("Logout", event -> handleLogout());
        logoutButton.addClassName("app-header__logout-button");
        logoutButton.setAriaLabel("Log out of your account");

        actionSection.add(themeGroup, logoutButton);
        return actionSection;
    }

    /**
     * Sets up the content area that child views will use.
     */
    private void setupContentArea() {
        contentArea.setSpacing(true);
        contentArea.setPadding(true);
        contentArea.setAlignItems(FlexComponent.Alignment.CENTER);
        contentArea.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        contentArea.addClassNames(
                LumoUtility.Flex.GROW,
                LumoUtility.Overflow.AUTO,
                "app-content");

        add(contentArea);
    }

    /**
     * Creates the application footer with copyright information.
     * WCAG AAA compliant contrast ratio (7:1 for text).
     */
    private void createFooter() {
        Footer footer = new Footer();
        footer.setWidthFull();
        footer.addClassName("app-footer");

        Div copyrightContainer = new Div();
        copyrightContainer.addClassName("app-footer__copyright");

        Span copyrightText = new Span("© 2026 Licensed to");
        Span companyLink = new Span("tusharkrishnatari.com");
        companyLink.addClassName("app-footer__company-link");
        Span rights = new Span("All rights reserved.");

        copyrightContainer.add(copyrightText, companyLink, rights);
        footer.add(copyrightContainer);
        add(footer);
    }

    /**
     * Adds content to the main content area.
     */
    public void setContent(com.vaadin.flow.component.Component... components) {
        contentArea.removeAll();
        contentArea.add(components);
    }

    /**
     * Gets the security service for use by child components.
     */
    public SecurityService getSecurityService() {
        return securityService;
    }

    /**
     * Handles logout action.
     */
    private void handleLogout() {
        securityService.logout();
        Notification.show("You have been logged out", 2000, Notification.Position.TOP_CENTER);
        UI.getCurrent().navigate(LoginView.class);
    }
}
