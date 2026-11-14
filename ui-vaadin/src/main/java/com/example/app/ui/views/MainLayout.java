package com.example.app.ui.views;

import com.example.app.ui.security.SecurityService;
import com.example.app.ui.theme.ThemeSwitcher;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.inject.Inject;

/**
 * Main application layout implementing RouterLayout for SPA behavior. The router automatically
 * swaps view content into the contentArea while header and footer remain persistent. WCAG 2.1 AAA
 * compliant with proper contrast ratios. Styles are defined in app-layout.css
 */
@StyleSheet("./styles/app-layout.css")
@StyleSheet("./styles/notifications.css")
public class MainLayout extends VerticalLayout implements RouterLayout, AfterNavigationObserver {

  private final SecurityService securityService;
  private final VerticalLayout contentArea;
  private Button loginButton;
  private HorizontalLayout userMenuTarget;

  @Inject
  public MainLayout(SecurityService securityService) {
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
   * Creates the application header with logo, title, theme selector, and logout button.
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
   * Creates the action section with theme selector, login button (anonymous), and user avatar with
   * context menu (authenticated).
   */
  private HorizontalLayout createActionSection() {
    HorizontalLayout actionSection = new HorizontalLayout();
    actionSection.setSpacing(true);
    actionSection.setAlignItems(FlexComponent.Alignment.CENTER);
    actionSection.addClassName("app-header__actions");

    // Theme selector — Lumo selected by default
    RadioButtonGroup<ThemeSwitcher.Theme> themeGroup = new RadioButtonGroup<>();
    themeGroup.setItems(ThemeSwitcher.Theme.values());
    themeGroup.setItemLabelGenerator(ThemeSwitcher.Theme::getDisplayName);
    themeGroup.addClassName("app-header__theme-selector");
    themeGroup.addValueChangeListener(event ->
        ThemeSwitcher.switchTheme(event.getValue()));
    themeGroup.setValue(ThemeSwitcher.getCurrentTheme());

    // Login button — shown when NOT authenticated
    this.loginButton = new Button("Login", event -> UI.getCurrent().navigate("login"));
    this.loginButton.addClassName("app-header__logout-button");
    this.loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    this.loginButton.setAriaLabel("Navigate to login page");

    // User avatar with dropdown — shown when authenticated
    this.userMenuTarget = new HorizontalLayout();
    this.userMenuTarget.setSpacing(true);
    this.userMenuTarget.setAlignItems(FlexComponent.Alignment.CENTER);
    this.userMenuTarget.addClassName("app-header__user-menu");
    this.userMenuTarget.getStyle().set("cursor", "pointer");

    String username = securityService.getAuthenticatedUsername().orElse("");
    Avatar avatar = new Avatar(username);
    avatar.addClassName("app-header__avatar");

    Span usernameLabel = new Span(username);
    usernameLabel.addClassName("app-header__username");

    this.userMenuTarget.add(avatar, usernameLabel);

    ContextMenu userMenu = new ContextMenu(this.userMenuTarget);
    userMenu.setOpenOnClick(true);
    userMenu.addItem("Profile", event -> { /* future: navigate to profile */ });
    userMenu.addItem("Settings", event -> { /* future: navigate to settings */ });
    userMenu.addItem("Logout", event -> handleLogout());

    actionSection.add(themeGroup, loginButton, userMenuTarget);
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
   * Creates the application footer with copyright information. WCAG AAA compliant contrast ratio
   * (7:1 for text).
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
   * Called by Vaadin's router to place the routed view inside the content area. This is what makes
   * the SPA behavior work — only contentArea swaps on navigation.
   */
  @Override
  public void showRouterLayoutContent(HasElement content) {
    contentArea.removeAll();
    contentArea.getElement().appendChild(content.getElement());
  }

  /**
   * Toggles header action visibility based on authentication state. Called by Vaadin after each
   * route navigation.
   */
  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    boolean authenticated = securityService.isAuthenticated();
    loginButton.setVisible(!authenticated);
    userMenuTarget.setVisible(authenticated);

    if (authenticated) {
      String username = securityService.getAuthenticatedUsername().orElse("");
      Avatar avatar = (Avatar) userMenuTarget.getComponentAt(0);
      Span usernameLabel = (Span) userMenuTarget.getComponentAt(1);
      avatar.setName(username);
      usernameLabel.setText(username);
    }
  }

  /**
   * Handles logout action.
   */
  private void handleLogout() {
    securityService.logout();
    Notification.show("You have been logged out", 2000, Notification.Position.TOP_CENTER);
    UI.getCurrent().navigate("login");
  }
}
