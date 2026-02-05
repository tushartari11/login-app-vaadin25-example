# Code Examples — Java 21 / Jakarta EE 10 / Vaadin 25

> Companion code examples for the guidelines in [CLAUDE.md](CLAUDE.md).

---

## Code Review Checklist

### Pattern Matching & Switch Expressions

```java
// Good: Modern pattern matching
return switch (shape) {
    case Circle c -> Math.PI * c.radius() * c.radius();
    case Rectangle r -> r.width() * r.height();
    case Triangle t -> 0.5 * t.base() * t.height();
};

// Avoid: Old-style casting
if (shape instanceof Circle) {
    Circle c = (Circle) shape;
    return Math.PI * c.radius() * c.radius();
}
```

### Records

```java
// Good: Clean record with validation
public record Customer(String id, String email) {
    public Customer {
        Objects.requireNonNull(id, "ID cannot be null");
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }
}
```

### Virtual Threads (Project Loom)

```java
// Good: Virtual threads for concurrent I/O
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Result>> futures = customers.stream()
        .map(customer -> executor.submit(() -> processCustomer(customer)))
        .toList();
}
```

### CDI (Contexts and Dependency Injection)

```java
@ApplicationScoped
public class CustomerService {

    @Inject
    private EntityManager em;

    @Inject
    private Event<CustomerCreatedEvent> customerEvent;

    @Transactional
    public Customer createCustomer(CustomerDTO dto) {
        Customer customer = new Customer(dto);
        em.persist(customer);
        customerEvent.fire(new CustomerCreatedEvent(customer));
        return customer;
    }
}
```

### JPA (Jakarta Persistence)

```java
@Entity
@NamedQuery(
    name = "Customer.findWithOrders",
    query = "SELECT c FROM Customer c JOIN FETCH c.orders WHERE c.active = true"
)
public class Customer {
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Order> orders;
}
```

### REST (Jakarta RESTful Web Services)

```java
@Path("/customers")
@ApplicationScoped
public class CustomerResource {

    @Inject
    private CustomerService service;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCustomer(@Valid CustomerDTO dto) {
        Customer customer = service.createCustomer(dto);
        URI location = URI.create("/customers/" + customer.getId());
        return Response.created(location).entity(customer).build();
    }
}
```

### Vaadin Component Architecture

```java
@Route("customers")
public class CustomerView extends VerticalLayout {

    private final CustomerService service;
    private final Grid<Customer> grid;

    public CustomerView(CustomerService service) {
        this.service = service;
        this.grid = new Grid<>(Customer.class);
        configureGrid();
        add(createToolbar(), grid);
        updateList();
    }

    private void configureGrid() {
        grid.setColumns("id", "name", "email");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.asSingleSelect().addValueChangeListener(evt -> editCustomer(evt.getValue()));
    }
}
```

### Vaadin Data Binding

```java
public class CustomerForm extends FormLayout {
    private final Binder<Customer> binder = new Binder<>(Customer.class);
    private final TextField name = new TextField("Name");
    private final EmailField email = new EmailField("Email");

    public CustomerForm() {
        add(name, email);
        binder.forField(name)
            .asRequired("Name is required")
            .withValidator(n -> n.length() >= 2, "Name must be at least 2 characters")
            .bind(Customer::getName, Customer::setName);
        binder.forField(email)
            .asRequired("Email is required")
            .bind(Customer::getEmail, Customer::setEmail);
    }
}
```

### Lumo Utility Classes (Java)

```java
// Good: Using LumoUtility constants
import com.vaadin.flow.theme.lumo.LumoUtility;

Div card = new Div();
card.addClassNames(
    LumoUtility.Padding.MEDIUM,
    LumoUtility.Margin.SMALL,
    LumoUtility.BorderRadius.LARGE,
    LumoUtility.BoxShadow.SMALL,
    LumoUtility.Background.BASE,
    LumoUtility.Display.FLEX,
    LumoUtility.FlexDirection.COLUMN,
    LumoUtility.Gap.MEDIUM
);

// Good: Text styling with utilities
Span errorMsg = new Span("Invalid email address");
errorMsg.addClassNames(
    LumoUtility.TextColor.ERROR,
    LumoUtility.FontSize.SMALL
);

// Good: Responsive layout with utilities
HorizontalLayout toolbar = new HorizontalLayout();
toolbar.addClassNames(
    LumoUtility.JustifyContent.BETWEEN,
    LumoUtility.AlignItems.CENTER,
    LumoUtility.Padding.Horizontal.MEDIUM,
    LumoUtility.Padding.Vertical.SMALL
);
```

### Theme Customization

```css
/* src/main/frontend/themes/my-theme/styles.css */

/* Global Lumo property overrides */
html {
  /* Brand colors */
  --lumo-primary-color: hsl(214, 90%, 48%);
  --lumo-primary-text-color: hsl(214, 90%, 42%);
  --lumo-primary-color-50pct: hsla(214, 90%, 48%, 0.5);
  --lumo-primary-color-10pct: hsla(214, 90%, 48%, 0.1);

  /* Typography */
  --lumo-font-family: "Inter", var(--lumo-font-family);
  --lumo-font-size-m: 0.9375rem; /* 15px base */

  /* Shape */
  --lumo-border-radius-m: 0.5rem;
  --lumo-border-radius-l: 0.75rem;

  /* Spacing adjustments */
  --lumo-space-m: 1rem;

  /* Cursor for clickable elements */
  --lumo-clickable-cursor: pointer;
}
```

### Dark Mode Support

```java
// Good: Enable dark mode toggle using Lumo variant
@Route("settings")
public class SettingsView extends VerticalLayout {

    public SettingsView() {
        Checkbox darkMode = new Checkbox("Dark mode");
        darkMode.addValueChangeListener(e -> {
            ThemeList themeList = UI.getCurrent().getElement().getThemeList();
            if (e.getValue()) {
                themeList.add(Lumo.DARK);
            } else {
                themeList.remove(Lumo.DARK);
            }
        });
        add(darkMode);
    }
}
```

### Compact Mode

```java
// Good: Enable compact mode for data-dense views
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@Theme("my-theme")
public class Application implements AppShellConfigurator {
}
```

### Styling Anti-Patterns

```java
// BAD: Hard-coded colors and sizes
button.getStyle().set("background-color", "#1976D2");
button.getStyle().set("color", "white");
button.getStyle().set("padding", "8px 16px");
button.getStyle().set("border-radius", "4px");
button.getStyle().set("font-size", "14px");

// GOOD: Use Lumo properties in CSS, utilities in Java
button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
button.addClassNames(
    LumoUtility.Padding.Horizontal.MEDIUM,
    LumoUtility.Padding.Vertical.SMALL
);
```

```css
/* BAD: Hard-coded values in CSS */
.my-card {
  background: #ffffff;
  color: #333333;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  font-size: 14px;
}

/* GOOD: Lumo properties — auto-adapts to dark mode and compact mode */
.my-card {
  background: var(--lumo-base-color);
  color: var(--lumo-body-text-color);
  padding: var(--lumo-space-m);
  border-radius: var(--lumo-border-radius-l);
  box-shadow: var(--lumo-box-shadow-s);
  font-size: var(--lumo-font-size-m);
}
```

---

## WCAG 2.1 AAA Accessibility Compliance

### 1.1 Text Alternatives

```java
// Good: Accessible image with alt text
Image logo = new Image("images/logo.png", "Company Logo");

// Good: Decorative image
Image decorative = new Image("images/divider.png", "");
decorative.getElement().setAttribute("role", "presentation");

// Good: Icon button with accessible name
Button closeBtn = new Button(VaadinIcon.CLOSE.create());
closeBtn.setAriaLabel("Close dialog");
closeBtn.getElement().setAttribute("title", "Close dialog");
```

### 1.3 Adaptable

```java
// Good: Semantic structure with landmarks
Main mainContent = new Main();
Nav navigation = new Nav();
navigation.getElement().setAttribute("aria-label", "Main navigation");

Header header = new Header();
Footer footer = new Footer();

// Good: Form field with autocomplete
TextField email = new TextField("Email");
email.getElement().setAttribute("autocomplete", "email");

TextField name = new TextField("Full Name");
name.getElement().setAttribute("autocomplete", "name");

// Good: Instructions not relying on sensory characteristics alone
Paragraph instruction = new Paragraph(
    "Required fields are marked with an asterisk (*) and have a red border."
);
```

### 1.4 Distinguishable — Enhanced Contrast & Visual Presentation

```java
// Good: High contrast theme support (7:1 ratio for AAA)
@CssImport("./styles/high-contrast-theme.css")
@Route("settings")
public class SettingsView extends VerticalLayout {

    public SettingsView() {
        Checkbox highContrastToggle = new Checkbox("Enable high contrast mode");
        highContrastToggle.addValueChangeListener(e -> {
            if (e.getValue()) {
                UI.getCurrent().getElement().getThemeList().add("high-contrast");
            } else {
                UI.getCurrent().getElement().getThemeList().remove("high-contrast");
            }
        });
        add(highContrastToggle);
    }
}
```

```css
/* high-contrast-theme.css — AAA compliant (7:1 ratio) using Lumo overrides */
:host([theme~="high-contrast"]) {
  --lumo-body-text-color: #000000;
  --lumo-header-text-color: #000000;
  --lumo-secondary-text-color: #1a1a1a;
  --lumo-tertiary-text-color: #333333;
  --lumo-primary-text-color: #00338a;
  --lumo-primary-color: #00338a;
  --lumo-primary-contrast-color: #ffffff;
  --lumo-error-color: #b50000;
  --lumo-error-text-color: #a00000;
  --lumo-success-color: #005e20;
  --lumo-success-text-color: #004d1a;
  --lumo-warning-text-color: #6b4c00;
  --lumo-contrast: #000000;
  --lumo-base-color: #ffffff;
}

/* Visual presentation (1.4.8): max line width, spacing — use Lumo space tokens */
.content-block {
  max-width: 80ch;
  line-height: 1.5;
  text-align: left;
  letter-spacing: 0.12em;
  word-spacing: 0.16em;
  padding: var(--lumo-space-m);
  font-size: var(--lumo-font-size-m);
}

/* Respect reduced motion preference (2.3.3) */
@media (prefers-reduced-motion: reduce) {
  * {
    animation: none !important;
    transition: none !important;
  }
}
```

### 2.1 Keyboard Accessible

```java
// Good: Keyboard accessible custom component
public class AccessibleCard extends Div {

    public AccessibleCard(String title, String content, Runnable action) {
        getElement().setAttribute("role", "button");
        getElement().setAttribute("tabindex", "0");
        setAriaLabel(title);

        // Handle both click and keyboard activation
        addClickListener(e -> action.run());
        getElement().addEventListener("keydown", e -> {
            // Activate on Enter or Space
        }).addEventData("event.key")
          .setFilter("event.key === 'Enter' || event.key === ' '");

        add(new H3(title), new Paragraph(content));
    }
}
```

### 2.2 Enough Time — Session Timeout

```java
// Good: Session timeout warning with data preservation
@ApplicationScoped
public class SessionTimeoutHandler {

    private static final int TIMEOUT_WARNING_SECONDS = 120;

    public void configureSessionWarning(UI ui) {
        // Warn users before session expires
        ui.addBeforeLeaveListener(event -> {
            if (hasUnsavedData(ui)) {
                event.postpone();
                showTimeoutDialog(ui, event);
            }
        });
    }

    private void showTimeoutDialog(UI ui, BeforeLeaveEvent event) {
        Dialog dialog = new Dialog();
        dialog.setAriaLabel("Session timeout warning");
        dialog.add(new Paragraph(
            "Your session will expire in " + TIMEOUT_WARNING_SECONDS +
            " seconds. Unsaved data will be preserved if you continue."
        ));

        Button continueBtn = new Button("Continue working", e -> {
            dialog.close();
            event.getContinueNavigationAction().proceed();
        });
        Button extendBtn = new Button("Extend session", e -> {
            extendSession(ui);
            dialog.close();
        });

        dialog.add(new HorizontalLayout(extendBtn, continueBtn));
        dialog.open();
    }
}
```

### 2.3 Seizures — Reduced Motion

```java
// Good: Respect prefers-reduced-motion
public class AnimationAwareLayout extends VerticalLayout {

    public AnimationAwareLayout() {
        // Allow users to disable animations from the application
        Checkbox reduceMotion = new Checkbox("Reduce motion and animations");
        reduceMotion.addValueChangeListener(e -> {
            if (e.getValue()) {
                UI.getCurrent().getElement().getThemeList().add("reduce-motion");
            } else {
                UI.getCurrent().getElement().getThemeList().remove("reduce-motion");
            }
        });
        add(reduceMotion);
    }
}
```

### 2.4 Navigable — Skip Navigation, Breadcrumbs, Headings

```java
// Good: Skip navigation, page title, breadcrumbs, headings
@Route("customers")
@PageTitle("Customer Management — MyApp")
public class CustomerView extends VerticalLayout {

    public CustomerView() {
        // Skip navigation link
        Anchor skipLink = new Anchor("#main-content", "Skip to main content");
        skipLink.addClassName("skip-link");

        // Breadcrumb navigation for location awareness (2.4.8)
        Nav breadcrumb = new Nav();
        breadcrumb.getElement().setAttribute("aria-label", "Breadcrumb");
        Span home = new Span();
        home.add(new Anchor("/", "Home"));
        Span separator = new Span(" / ");
        Span current = new Span("Customers");
        current.getElement().setAttribute("aria-current", "page");
        breadcrumb.add(home, separator, current);

        // Section headings (2.4.10)
        H1 pageTitle = new H1("Customer Management");
        H2 filterSection = new H2("Search and Filters");
        H2 resultsSection = new H2("Customer List");

        // Main content landmark
        Div mainContent = new Div();
        mainContent.setId("main-content");
        mainContent.getElement().setAttribute("role", "main");

        // Descriptive link text (2.4.9)
        Anchor addLink = new Anchor("/customers/new", "Add a new customer");
        // Avoid: new Anchor("/customers/new", "Click here");

        add(skipLink, breadcrumb, pageTitle, filterSection, mainContent, resultsSection, addLink);
    }
}
```

```css
/* Skip link — visible only on focus, using Lumo tokens */
.skip-link {
  position: absolute;
  top: -40px;
  left: 0;
  padding: var(--lumo-space-xs) var(--lumo-space-m);
  background: var(--lumo-contrast);
  color: var(--lumo-base-color);
  z-index: 100;
  font-weight: bold;
  font-size: var(--lumo-font-size-s);
  border-radius: var(--lumo-border-radius-s);
}
.skip-link:focus {
  top: 0;
}

/* Visible focus indicator (2.4.7) using Lumo primary */
*:focus-visible {
  outline: 3px solid var(--lumo-primary-color);
  outline-offset: 2px;
}
```

### 2.5 Input Modalities — Target Size

```java
// Good: Sufficient target size and label-in-name match
Button submitBtn = new Button("Submit Order");
// Accessible name matches visible label
submitBtn.getStyle().set("min-width", "44px");
submitBtn.getStyle().set("min-height", "44px");
submitBtn.getStyle().set("padding", "0.75em 1.5em");
```

```css
/* Global minimum target size for interactive elements (2.5.5 AAA: 44x44px) */
/* Note: --lumo-size-m is 36px by default; override or use min-width/height */
vaadin-button,
vaadin-checkbox,
vaadin-radio-button,
vaadin-text-field,
vaadin-select,
a[href] {
  min-width: 44px;
  min-height: 44px;
}
```

### 3.1 Readable — Language & Abbreviations

```java
// Good: Set page language
@Route("")
public class MainView extends VerticalLayout implements HasDynamicTitle {

    public MainView() {
        // Set document language
        UI.getCurrent().getElement().setAttribute("lang", "en");

        // Mark language changes within content (3.1.2)
        Span germanText = new Span("Willkommen");
        germanText.getElement().setAttribute("lang", "de");

        // Abbreviations with expansion (3.1.4)
        Span abbr = new Span("WCAG");
        abbr.getElement().setAttribute("title", "Web Content Accessibility Guidelines");
        // Or provide inline: "Web Content Accessibility Guidelines (WCAG)"

        // Glossary link for unusual terms (3.1.3)
        Anchor glossaryLink = new Anchor("/glossary", "View glossary of technical terms");
    }

    @Override
    public String getPageTitle() {
        return "Home — MyApp";
    }
}
```

### 3.2 Predictable — No Auto-Context Change

```java
// Good: No auto-submit on input change (3.2.2, 3.2.5)
Select<String> categoryFilter = new Select<>();
categoryFilter.setLabel("Filter by category");
categoryFilter.setItems("All", "Active", "Inactive");
// Do NOT auto-navigate or auto-submit on value change
// Instead, require explicit action:
Button applyFilter = new Button("Apply Filter", e -> {
    applyFilter(categoryFilter.getValue());
});

// Bad: Auto-context-change on selection
// categoryFilter.addValueChangeListener(e -> navigateTo(e.getValue()));
```

### 3.3 Input Assistance — Form Validation & Error Prevention

```java
// Good: Comprehensive form validation with help text and error prevention
public class AccessibleForm extends FormLayout {

    private final Binder<Customer> binder = new Binder<>(Customer.class);

    public AccessibleForm() {
        // Labels and instructions (3.3.2)
        TextField name = new TextField("Full Name");
        name.setHelperText("Enter your first and last name");            // Help (3.3.5)
        name.setRequiredIndicatorVisible(true);
        name.getElement().setAttribute("autocomplete", "name");          // Input purpose (1.3.5)

        EmailField email = new EmailField("Email Address");
        email.setHelperText("Example: user@domain.com");                 // Help (3.3.5)
        email.setErrorMessage("Please enter a valid email address");     // Error suggestion (3.3.3)
        email.getElement().setAttribute("autocomplete", "email");

        // Binder with accessible error messages (3.3.1, 3.3.3)
        binder.forField(name)
            .asRequired("Name is required. Please enter your full name.")
            .withValidator(n -> n.length() >= 2,
                "Name must be at least 2 characters. Please enter your full name.")
            .bind(Customer::getName, Customer::setName);

        binder.forField(email)
            .asRequired("Email is required. Please enter your email address.")
            .withValidator(new EmailValidator(
                "Invalid email format. Please use format: user@domain.com"))
            .bind(Customer::getEmail, Customer::setEmail);

        // Error prevention: confirmation before submit (3.3.6)
        Button submitBtn = new Button("Submit", e -> {
            if (binder.validate().isOk()) {
                showConfirmationDialog();  // Review before final submit
            }
        });
        submitBtn.getStyle().set("min-height", "44px");

        add(name, email, submitBtn);
    }

    private void showConfirmationDialog() {
        Dialog confirm = new Dialog();
        confirm.setAriaLabel("Confirm submission");
        confirm.setCloseOnOutsideClick(false);
        confirm.add(new H2("Review Your Submission"));
        confirm.add(new Paragraph("Please review the information below before submitting."));
        // Display form data summary for review
        confirm.add(new Button("Confirm and Submit", e -> {
            save();
            confirm.close();
        }));
        confirm.add(new Button("Go Back and Edit", e -> confirm.close()));
        confirm.open();
    }
}
```

### 4.1 Compatible — ARIA Live Regions & Custom Components

```java
// Good: ARIA live region for status messages (4.1.3)
public class StatusNotifier extends Div {

    private final Div statusRegion;

    public StatusNotifier() {
        statusRegion = new Div();
        statusRegion.getElement().setAttribute("role", "status");
        statusRegion.getElement().setAttribute("aria-live", "polite");
        statusRegion.getElement().setAttribute("aria-atomic", "true");
        add(statusRegion);
    }

    public void announceSuccess(String message) {
        statusRegion.setText(message);
    }

    public void announceError(String message) {
        statusRegion.getElement().setAttribute("role", "alert");
        statusRegion.getElement().setAttribute("aria-live", "assertive");
        statusRegion.setText(message);
    }
}

// Good: Custom component with proper name, role, value (4.1.2)
public class StarRating extends Div {

    public StarRating(int initialValue, int max) {
        getElement().setAttribute("role", "slider");
        getElement().setAttribute("aria-valuemin", "0");
        getElement().setAttribute("aria-valuemax", String.valueOf(max));
        getElement().setAttribute("aria-valuenow", String.valueOf(initialValue));
        getElement().setAttribute("aria-label", "Rating");
        getElement().setAttribute("tabindex", "0");
    }

    public void setValue(int value) {
        getElement().setAttribute("aria-valuenow", String.valueOf(value));
    }
}
```

---

## Best Practices Guidelines

### Layered Architecture

```
Presentation Layer (Vaadin Views)
    ↓
Service Layer (Business Logic)
    ↓
Repository/DAO Layer (Data Access)
    ↓
Domain Model (Entities)
```

### Package Structure

```
com.company.project
├── domain          # Entities, value objects
├── repository      # Data access interfaces
├── service         # Business logic
├── api             # REST endpoints
├── views           # Vaadin UI views
├── components      # Reusable UI components
└── config          # Configuration classes
```

### Method Design

```java
// Good: Single responsibility, clear intent
public Optional<Customer> findActiveCustomerByEmail(String email) {
    if (email == null || email.isBlank()) {
        return Optional.empty();
    }
    return customerRepository.findByEmailAndActive(email, true);
}

// Avoid: Too many responsibilities
public Customer findAndUpdateCustomerStatus(String email, boolean active, LocalDate date) {
    // Doing too much in one method
}
```

### Exception Handling

```java
@ApplicationException(rollback = true)
public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String customerId) {
        super("Customer not found: " + customerId);
    }
}
```

### Null Safety

```java
// Good: Clear intent with Optional
public Optional<Customer> findById(String id) {
    return Optional.ofNullable(em.find(Customer.class, id));
}

// Good: Never null for collections
public List<Order> findOrdersByCustomer(String customerId) {
    return orderRepository.findByCustomerId(customerId)
        .orElse(List.of()); // Empty list, not null
}
```

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @InjectMocks
    private CustomerService service;

    @Test
    void shouldCreateCustomerWithValidData() {
        // Arrange
        CustomerDTO dto = new CustomerDTO("John", "john@example.com");
        Customer expected = new Customer("1", "John", "john@example.com");
        when(repository.save(any(Customer.class))).thenReturn(expected);

        // Act
        Customer result = service.createCustomer(dto);

        // Assert
        assertNotNull(result);
        assertEquals("John", result.getName());
        verify(repository).save(any(Customer.class));
    }
}
```

### Automated Accessibility Testing

```java
// Integration test with axe-core via Selenium
@ExtendWith(SeleniumExtension.class)
class AccessibilityTest {

    @Test
    void customerViewShouldBeAccessible(ChromeDriver driver) {
        driver.get("http://localhost:8080/customers");

        // Run axe-core accessibility scan
        AxeBuilder axeBuilder = new AxeBuilder()
            .withTags(List.of("wcag2a", "wcag2aa", "wcag2aaa", "wcag21aa", "wcag21aaa"));

        Results results = axeBuilder.analyze(driver);

        // Assert no violations
        assertTrue(results.getViolations().isEmpty(),
            "Accessibility violations found: " +
            results.getViolations().stream()
                .map(v -> v.getId() + ": " + v.getDescription())
                .collect(Collectors.joining("\n")));
    }
}
```

---

## Dependency Management

### Maven Dependency Hierarchy

```xml
<dependencyManagement>
    <!-- Define versions in one place -->
    <dependencies>
        <dependency>
            <groupId>jakarta.platform</groupId>
            <artifactId>jakarta.jakartaee-bom</artifactId>
            <version>10.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>com.vaadin</groupId>
            <artifactId>vaadin-bom</artifactId>
            <version>25.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Essential Dependencies Only

```xml
<dependencies>
    <!-- Jakarta EE API -->
    <dependency>
        <groupId>jakarta.platform</groupId>
        <artifactId>jakarta.jakartaee-api</artifactId>
        <scope>provided</scope>
    </dependency>

    <!-- Vaadin -->
    <dependency>
        <groupId>com.vaadin</groupId>
        <artifactId>vaadin</artifactId>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Code Review Process

### Providing Feedback

```
✓ Good: "Consider using Optional here to make the nullable return explicit:
         `Optional<Customer> findById(String id)`"

✓ Good: "This method violates SRP by both fetching and transforming data.
         Consider splitting into findCustomer() and toDTO()."

✗ Avoid: "This is wrong."
✗ Avoid: "Why didn't you use Optional here?"
```

---

## Metrics & Code Quality

### Build Configuration

```xml
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-enforcer-plugin</artifactId>
        <executions>
            <execution>
                <id>enforce-versions</id>
                <goals>
                    <goal>enforce</goal>
                </goals>
                <configuration>
                    <rules>
                        <requireJavaVersion>
                            <version>[21,)</version>
                        </requireJavaVersion>
                        <dependencyConvergence/>
                    </rules>
                </configuration>
            </execution>
        </executions>
    </plugin>
</plugins>
```
