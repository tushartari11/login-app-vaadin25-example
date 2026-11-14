package com.example.app.ui.views;

import com.example.app.ui.security.SecurityService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * Login view with form-based authentication. WCAG 2.1 AAA compliant: accessible labels, keyboard
 * navigation, error messages, and high contrast.
 */
@Route(value = "login", layout = MainLayout.class)
@PageTitle("Login | Vaadin App")
@AnonymousAllowed
@StyleSheet("./styles/login-view.css")
public class LoginView extends VerticalLayout {

  @Inject
  private SecurityService securityService;

  private TextField usernameField;
  private PasswordField passwordField;
  private Button loginButton;
  private Paragraph errorMessage;

  @PostConstruct
  public void init() {
    if (securityService.isAuthenticated()) {
      UI.getCurrent().navigate(MainView.class);
      return;
    }

    setupLayout();
    createHeader();
    createLoginForm();
    createFooter();
  }

  private void setupLayout() {
    setSpacing(false);
    setPadding(true);
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);
    setSizeFull();
  }

  private void createHeader() {
    H1 title = new H1("Login to Your Account");
    title.addClassNames(
        LumoUtility.FontSize.XXXLARGE,
        LumoUtility.Margin.Bottom.MEDIUM,
        LumoUtility.TextColor.PRIMARY);
    title.setId("login-title");

    add(title);
  }

  private void createLoginForm() {
    VerticalLayout formLayout = new VerticalLayout();
    formLayout.setSpacing(true);
    formLayout.setPadding(true);
    formLayout.setWidth("400px");
    formLayout.addClassNames(
        LumoUtility.Background.BASE,
        LumoUtility.BorderRadius.LARGE,
        LumoUtility.BoxShadow.MEDIUM,
        LumoUtility.Padding.LARGE);

    usernameField = new TextField("Username");
    usernameField.setPlaceholder("Enter your username");
    usernameField.setWidthFull();
    usernameField.setRequired(true);
    usernameField.setAutofocus(true);
    usernameField.setHelperText("Your username is case-sensitive");
    usernameField.setId("username-field");
    usernameField.setAriaLabel("Username (required)");

    passwordField = new PasswordField("Password");
    passwordField.setPlaceholder("Enter your password");
    passwordField.setWidthFull();
    passwordField.setRequired(true);
    passwordField.setHelperText("Password is case-sensitive");
    passwordField.setId("password-field");
    passwordField.setAriaLabel("Password (required)");

    errorMessage = new Paragraph();
    errorMessage.addClassNames(
        LumoUtility.TextColor.ERROR,
        LumoUtility.FontSize.SMALL,
        LumoUtility.Margin.Vertical.XSMALL);
    errorMessage.setVisible(false);
    errorMessage.getElement().setAttribute("role", "alert");
    errorMessage.getElement().setAttribute("aria-live", "assertive");

    loginButton = new Button("Login", event -> handleLogin());
    loginButton.setWidthFull();
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    loginButton.addClassNames(LumoUtility.Margin.Top.MEDIUM);
    loginButton.setId("login-button");
    loginButton.addClickShortcut(Key.ENTER);

    formLayout.add(usernameField, passwordField, errorMessage, loginButton);
    add(formLayout);
  }

  private void createFooter() {
    RouterLink registerLink = new RouterLink("Don't have an account? Register here",
        RegistrationView.class);
    registerLink.addClassNames(
        LumoUtility.Margin.Top.MEDIUM,
        LumoUtility.TextColor.SECONDARY);
    registerLink.getElement().setAttribute("aria-label", "Navigate to registration page");

    add(registerLink);
  }

  private void handleLogin() {
    String username = usernameField.getValue().trim();
    String password = passwordField.getValue();

    if (username.isBlank()) {
      showError("Please enter your username");
      usernameField.focus();
      return;
    }
    if (password.isBlank()) {
      showError("Please enter your password");
      passwordField.focus();
      return;
    }

    try {
      boolean success = securityService.authenticate(username, password);
      if (success) {
        Notification notification = Notification.show(
            "Welcome back, " + username + "!",
            3000,
            Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.getElement().setAttribute("role", "status");
        notification.getElement().setAttribute("aria-live", "polite");

        UI.getCurrent().navigate(MainView.class);
      } else {
        showError("Invalid username or password. Please try again.");
        passwordField.clear();
        usernameField.focus();
      }
    } catch (Exception e) {
      showError("Login failed. Please try again later.");
    }
  }

  private void showError(String message) {
    errorMessage.setText(message);
    errorMessage.setVisible(true);
  }
}
