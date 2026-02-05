package com.example.vaadin.views;

import com.example.vaadin.security.SecurityService;
import com.example.vaadin.services.RegistrationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.component.dependency.StyleSheet;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * User registration view with validation and WCAG 2.1 AAA compliance.
 * Provides accessible form fields, error handling, and keyboard navigation.
 */
@Route("register")
@PageTitle("Register | Vaadin App")
@AnonymousAllowed
@StyleSheet("./styles/registration-view.css")
public class RegistrationView extends AppLayout {

    @Inject
    private RegistrationService registrationService;

    @Inject
    public RegistrationView(SecurityService securityService) {
        super(securityService);
    }

    private TextField usernameField;
    private EmailField emailField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private Button registerButton;
    private Paragraph errorMessage;

    private Binder<RegistrationFormData> binder;

    @PostConstruct
    public void init() {
        setupLayout();
        createHeader();
        createRegistrationForm();
        createFooter();
        setupValidation();
    }

    private void setupLayout() {
        setSpacing(false);
        setPadding(true);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();
        addClassName(LumoUtility.Background.CONTRAST_5);
    }

    private void createHeader() {
        H1 title = new H1("Create Your Account");
        title.addClassNames(
                LumoUtility.FontSize.XXXLARGE,
                LumoUtility.Margin.Bottom.MEDIUM,
                LumoUtility.TextColor.PRIMARY);
        add(title);
    }

    private void createRegistrationForm() {
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
        usernameField.setPlaceholder("Choose a username");
        usernameField.setWidthFull();
        usernameField.setRequired(true);
        usernameField.setAutofocus(true);
        usernameField.setHelperText("3-50 characters, letters, numbers, underscores, and hyphens only");
        usernameField.setAriaLabel("Username (required, 3 to 50 characters)");

        emailField = new EmailField("Email");
        emailField.setPlaceholder("your.email@example.com");
        emailField.setWidthFull();
        emailField.setRequired(true);
        emailField.setHelperText("We'll send a verification email to this address");
        emailField.setAriaLabel("Email address (required)");

        passwordField = new PasswordField("Password");
        passwordField.setPlaceholder("Create a strong password");
        passwordField.setWidthFull();
        passwordField.setRequired(true);
        passwordField.setHelperText("""
                At least 8 characters with:
                • One uppercase letter
                • One lowercase letter
                • One digit
                • One special character
                """);
        passwordField.setAriaLabel(
                "Password (required, minimum 8 characters with uppercase, lowercase, digit, and special character)");

        confirmPasswordField = new PasswordField("Confirm Password");
        confirmPasswordField.setPlaceholder("Re-enter your password");
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setHelperText("Must match the password above");
        confirmPasswordField.setAriaLabel("Confirm password (required)");

        errorMessage = new Paragraph();
        errorMessage.addClassNames(
                LumoUtility.TextColor.ERROR,
                LumoUtility.FontSize.SMALL,
                LumoUtility.Margin.Vertical.XSMALL);
        errorMessage.setVisible(false);
        errorMessage.getElement().setAttribute("role", "alert");
        errorMessage.getElement().setAttribute("aria-live", "assertive");

        registerButton = new Button("Create Account", event -> handleRegistration());
        registerButton.setWidthFull();
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        formLayout.add(
                usernameField,
                emailField,
                passwordField,
                confirmPasswordField,
                errorMessage,
                registerButton);
        add(formLayout);
    }

    private void createFooter() {
        RouterLink loginLink = new RouterLink("Already have an account? Login here", LoginView.class);
        loginLink.addClassNames(
                LumoUtility.Margin.Top.MEDIUM,
                LumoUtility.TextColor.SECONDARY);
        loginLink.getElement().setAttribute("aria-label", "Navigate to login page");
        add(loginLink);
    }

    private void setupValidation() {
        binder = new Binder<>(RegistrationFormData.class);

        binder.forField(usernameField)
                .withValidator(new StringLengthValidator(
                        "Username must be 3-50 characters",
                        3, 50))
                .withValidator(new RegexpValidator(
                        "Username can only contain letters, numbers, underscores, and hyphens",
                        "^[a-zA-Z0-9_-]+$"))
                .withValidator(username -> registrationService.isUsernameAvailable(username),
                        "Username is already taken")
                .bind(RegistrationFormData::getUsername, RegistrationFormData::setUsername);

        binder.forField(emailField)
                .withValidator(new EmailValidator("Please enter a valid email address"))
                .withValidator(email -> registrationService.isEmailAvailable(email),
                        "Email is already registered")
                .bind(RegistrationFormData::getEmail, RegistrationFormData::setEmail);

        binder.forField(passwordField)
                .withValidator(password -> password.length() >= 8,
                        "Password must be at least 8 characters long")
                .bind(RegistrationFormData::getPassword, RegistrationFormData::setPassword);

        binder.forField(confirmPasswordField)
                .withValidator(confirmPassword -> confirmPassword.equals(passwordField.getValue()),
                        "Passwords do not match")
                .bind(RegistrationFormData::getConfirmPassword, RegistrationFormData::setConfirmPassword);
    }

    private void handleRegistration() {
        try {
            RegistrationFormData formData = new RegistrationFormData();
            binder.writeBean(formData);

            registrationService.registerNewUser(
                    formData.getUsername(),
                    formData.getEmail(),
                    formData.getPassword());

            Notification notification = Notification.show(
                    "Registration successful! Please log in with your credentials.",
                    5000,
                    Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            notification.getElement().setAttribute("role", "status");
            notification.getElement().setAttribute("aria-live", "polite");

            UI.getCurrent().navigate(LoginView.class);

        } catch (ValidationException e) {
            showError("Please correct the errors in the form");
        } catch (jakarta.validation.ValidationException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Registration failed. Please try again later.");
        }
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisible(true);
    }

    public static class RegistrationFormData {
        private String username;
        private String email;
        private String password;
        private String confirmPassword;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}
