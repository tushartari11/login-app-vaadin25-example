# Java 21 / Jakarta EE 10 / Vaadin 25 Code Review & Best Practices Guide

> Code examples for all guidelines below are in [CODING-GUIDELINES.md](CODING-GUIDELINES.md).

## Overview

This skill provides comprehensive guidelines for code review, best practices, and coding standards for projects using Java 21, Jakarta EE 10, and Vaadin 25. The focus is on maintaining high code quality, minimizing dependencies, adhering to enterprise standards, and achieving **WCAG 2.1 AAA accessibility conformance** across all user-facing content.

## Target Audience

Senior Java developers and architects working with modern Jakarta EE applications and Vaadin-based frontends.

## Core Principles

### 1. Dependency Minimalism

- Keep the dependency tree as small as possible
- Avoid duplicate functionality across dependencies
- Prefer Jakarta EE standard APIs over third-party libraries when possible
- Regularly audit and prune unused dependencies
- Use dependency convergence to avoid version conflicts

### 2. Standards Compliance

- Strict adherence to Jakarta EE 10 specifications
- Follow Java language conventions and idioms for Java 21
- Utilize Vaadin 25 best practices for UI components
- Maintain consistency with established patterns

### 3. Code Quality Focus

- Prioritize readability and maintainability
- Embrace modern Java features appropriately
- Write self-documenting code with clear intent
- Minimize complexity and cognitive load

### 4. Accessibility First (WCAG 2.1 AAA)

- Target WCAG 2.1 AAA conformance for all user-facing content
- Treat accessibility as a core requirement, not an afterthought
- Use semantic HTML and ARIA attributes correctly
- Test with assistive technologies (screen readers, keyboard-only navigation)
- Provide multiple ways to access all content (text, audio, visual)
- Ensure all interactive components meet minimum 44x44 CSS pixel target size

---

## Code Review Checklist

### Simplicity Principles

- [ ] Keep the design KISS and YAGNI
- [ ] Always implement the simplest possible solution
- [ ] Write simple code first; ask before implementing enhancements or optional features
- [ ] Never over-engineer; ask about adding optional features or extension points
- [ ] Create new components with minimal business logic and essential fields only

### Code Style

- [ ] Prefer multiple simpler lines to one more complex line
- [ ] Prefer multiline Strings (text blocks) over String concatenations
- [ ] Prefer imports over fully qualified class names
- [ ] Use "this" to reference instance fields
- [ ] Remove unused imports
- [ ] Extract variables to eliminate duplication
- [ ] Prefer enums over plain Strings for finite, well-defined values
- [ ] Reuse enum constants as values if possible; enum constants do not have to follow naming conventions
- [ ] Prefer try-with-resources over explicitly closing resources

### Java 21 Language Features

#### ✓ Pattern Matching & Switch Expressions

- [ ] Use pattern matching for `instanceof` to eliminate casting
- [ ] Leverage switch expressions for concise, exhaustive handling
- [ ] Utilize record patterns for deconstructing complex data
- [ ] Apply sealed classes for controlled inheritance hierarchies

#### ✓ Records

- [ ] Use records for immutable data carriers
- [ ] Validate in compact constructors for invariants
- [ ] Avoid adding mutable state to records
- [ ] Consider records for DTOs and value objects

#### ✓ Virtual Threads (Project Loom)

- [ ] Use virtual threads for I/O-bound operations
- [ ] Start virtual threads with `Thread.startVirtualThread()` or `Executors.newVirtualThreadPerTaskExecutor()`
- [ ] Avoid blocking operations in platform threads
- [ ] Do not pool virtual threads (they're cheap to create)

#### ✓ Sequenced Collections

- [ ] Use `SequencedCollection`, `SequencedSet`, `SequencedMap` for ordered collections
- [ ] Leverage `getFirst()`, `getLast()`, `reversed()` methods
- [ ] Prefer these over legacy index-based access

### Other Java Version & Syntax

- [ ] Use Java 21 with modern syntax (var, pattern matching, records, text blocks)
- [ ] Prefer dependencies in this order: Java SE, MicroProfile, Jakarta EE
- [ ] Use Java SE APIs over writing custom code
- [ ] Prefer unchecked over checked exceptions; never throw generic exceptions like java.lang.Exception
- [ ] Throw RuntimeException subclasses, not directly; inherit from WebApplicationException in JAX-RS projects
- [ ] Consider using Java records instead of classes with final fields
- [ ] Prefer factory methods in records over passing null in constructors

#### Stream & Collections

- [ ] Prefer java.util.stream.Stream API over for loops
- [ ] Avoid forEach; prefer Stream methods
- [ ] Prefer Stream.of to Arrays.stream
- [ ] Prefer toList() to .collect(Collectors.toList())
- [ ] Prefer List.of over String[] or new ArrayList<>()
- [ ] Avoid creating unnecessary intermediate collections when streaming arrays
- [ ] Prefer variable declaration over lengthy method chaining

#### JavaDoc

- [ ] Do not write obvious JavaDoc comments that rephrase code
- [ ] Document the intentions and the "why", not implementation details
- [ ] Either describe the "why" or do not comment at all
- [ ] Follow links in JavaDoc to external specifications and use them for code generation
- [ ] Use popular, also funny, technical terms from the Java SE, MicroProfile and Jakarta EE ecosystems as examples in unit tests and javadoc

### Jakarta EE 10 Standards

#### ✓ CDI (Contexts and Dependency Injection)

- [ ] Use `@Inject` over `@Autowired` (avoid Spring annotations)
- [ ] Properly scope beans: `@RequestScoped`, `@SessionScoped`, `@ApplicationScoped`
- [ ] Avoid `@Dependent` scope unless necessary (default but creates new instances)
- [ ] Use producers for complex bean creation
- [ ] Leverage qualifiers for disambiguation
- [ ] Use events (`@Observes`) for loose coupling

#### ✓ JPA (Jakarta Persistence)

- [ ] Use constructor expressions in JPQL for DTOs
- [ ] Leverage `@NamedQuery` for reusable queries
- [ ] Avoid N+1 queries with `JOIN FETCH`
- [ ] Use pagination for large result sets
- [ ] Properly configure fetch types: `LAZY` by default
- [ ] Use `@EntityGraph` for dynamic fetching strategies

#### ✓ REST (Jakarta RESTful Web Services)

- [ ] Use proper HTTP methods: GET (idempotent), POST, PUT, DELETE
- [ ] Return appropriate status codes
- [ ] Use `@Valid` for request validation
- [ ] Leverage `Response` builder for complex responses
- [ ] Version APIs appropriately
- [ ] Document with OpenAPI annotations

#### ✓ Bean Validation

- [ ] Use Jakarta Validation annotations: `@NotNull`, `@NotEmpty`, `@Size`, `@Email`
- [ ] Create custom validators for complex rules
- [ ] Validate at API boundaries
- [ ] Group validations when needed

#### ✓ Transactions

- [ ] Use `@Transactional` for declarative transaction management
- [ ] Set appropriate transaction attributes: `REQUIRED`, `REQUIRES_NEW`, `MANDATORY`
- [ ] Handle `@ApplicationException` for rollback control
- [ ] Keep transactions short and focused

### Vaadin 25 UI Guidelines

#### ✓ Component Architecture

- [ ] Create reusable custom components extending Vaadin components
- [ ] Use composition over inheritance
- [ ] Keep views lightweight, delegate business logic to services
- [ ] Properly scope UI components (usually `@Route` with no CDI scope)

#### ✓ Data Binding

- [ ] Use `Binder` for form data binding and validation
- [ ] Leverage `DataProvider` for efficient data loading
- [ ] Use lazy loading for large datasets
- [ ] Implement proper filtering and sorting

#### ✓ Lumo Theme & Styling

Vaadin 25 uses **Lumo** as its default and recommended theme. Always use Lumo CSS custom properties (`--lumo-*`) instead of hard-coded values to ensure consistency, dark mode support, and compact mode compatibility.

- [ ] Use Lumo CSS custom properties for all styling — never hard-code colors, sizes, or fonts
- [ ] Use `LumoUtility` Java constants for utility class names instead of raw CSS strings
- [ ] Apply theme variants via `addThemeVariants()` on components (e.g., `GridVariant.LUMO_ROW_STRIPES`)
- [ ] Support dark mode by using Lumo color properties (they auto-adjust per scheme)
- [ ] Use Lumo spacing/sizing scale for consistent layout
- [ ] Customize the theme by overriding `--lumo-*` properties in a theme stylesheet, not by writing component-level CSS selectors
- [ ] Enable compact mode via `@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")` where appropriate

##### Lumo CSS Custom Properties Reference

**Colors:**
| Property | Usage |
|----------|-------|
| `--lumo-base-color` | Page/surface background |
| `--lumo-contrast` / `--lumo-contrast-5pct` to `90pct` | Grayscale, borders, subtle backgrounds |
| `--lumo-primary-color` / `--lumo-primary-text-color` / `--lumo-primary-contrast-color` | Primary actions, links |
| `--lumo-error-color` / `--lumo-error-text-color` / `--lumo-error-contrast-color` | Error states |
| `--lumo-warning-color` / `--lumo-warning-text-color` / `--lumo-warning-contrast-color` | Warning states |
| `--lumo-success-color` / `--lumo-success-text-color` / `--lumo-success-contrast-color` | Success states |
| `--lumo-header-text-color` | Headings |
| `--lumo-body-text-color` | Body text |
| `--lumo-secondary-text-color` | Secondary/muted text |
| `--lumo-tertiary-text-color` | Tertiary/hint text |
| `--lumo-disabled-text-color` | Disabled elements |

**Typography:**
| Property | Usage |
|----------|-------|
| `--lumo-font-family` | Base font family |
| `--lumo-font-size-xxs` to `--lumo-font-size-xxxl` | Font size scale (xxs, xs, s, m, l, xl, xxl, xxxl) |
| `--lumo-line-height-xs` / `s` / `m` | Line height scale |

**Size & Space:**
| Property | Usage |
|----------|-------|
| `--lumo-size-xs` to `--lumo-size-xl` | Component sizing (xs, s, m, l, xl) |
| `--lumo-space-xs` to `--lumo-space-xl` | Square spacing |
| `--lumo-space-wide-xs` to `--lumo-space-wide-xl` | Horizontal-biased spacing |
| `--lumo-space-tall-xs` to `--lumo-space-tall-xl` | Vertical-biased spacing |
| `--lumo-icon-size-s` / `m` / `l` | Icon sizing (relative to font size) |

**Shape, Elevation & Interaction:**
| Property | Usage |
|----------|-------|
| `--lumo-border-radius-s` / `m` / `l` | Border radius scale |
| `--lumo-box-shadow-xs` to `--lumo-box-shadow-xl` | Elevation/shadow scale |
| `--lumo-clickable-cursor` | Cursor for clickable elements (default: `default`) |

##### Lumo Utility Classes (Java)

Use `LumoUtility` nested constants in Java instead of raw CSS class name strings.

**Available `LumoUtility` categories:**

- `Background` — base, contrast, primary, error, warning, success
- `BorderRadius` — SMALL, MEDIUM, LARGE
- `BoxShadow` — XS, S, M, L, XL
- `Display` — FLEX, GRID, BLOCK, INLINE, HIDDEN
- `FlexDirection` — ROW, COLUMN, ROW_REVERSE, COLUMN_REVERSE
- `AlignItems` / `AlignSelf` / `JustifyContent` — alignment utilities
- `FlexWrap` — WRAP, NOWRAP
- `FontSize` — XXSMALL through XXXLARGE
- `FontWeight` — THIN, EXTRALIGHT, LIGHT, NORMAL, MEDIUM, SEMIBOLD, BOLD, EXTRABOLD
- `Gap` — XSMALL through XLARGE (also `Gap.Row`, `Gap.Column`)
- `Grid.Column` / `Grid.Row` — grid column/row spans
- `Height` / `Width` / `MaxHeight` / `MaxWidth` / `MinHeight` / `MinWidth` — sizing
- `IconSize` — SMALL, MEDIUM, LARGE
- `Margin` / `Padding` — all directions including `Horizontal`, `Vertical`, `Left`, `Right`, `Top`, `Bottom`
- `Overflow` — AUTO, HIDDEN, SCROLL
- `Position` — ABSOLUTE, FIXED, RELATIVE, STICKY
- `TextAlignment` — LEFT, CENTER, RIGHT, JUSTIFY
- `TextColor` — PRIMARY, SECONDARY, TERTIARY, DISABLED, ERROR, WARNING, SUCCESS, HEADER, BODY
- `TextOverflow` — ELLIPSIS, CLIP
- `TextTransform` — UPPERCASE, LOWERCASE, CAPITALIZE, NONE
- `Accessibility` — SCREEN_READER_ONLY (hides visually but keeps accessible)

##### Theme Customization

Override Lumo properties globally in your theme stylesheet.

##### Dark Mode Support

##### Compact Mode

##### Styling Anti-Patterns

#### ✓ Security

- [ ] Use `@PermitAll`, `@RolesAllowed`, `@DenyAll` on views
- [ ] Implement proper authentication with Jakarta Security
- [ ] Validate all user inputs server-side
- [ ] Use CSRF protection (enabled by default)
- [ ] Configure Content Security Policy

#### ✓ Accessibility (Vaadin Components)

- [ ] Set `aria-label` on all icon-only buttons and non-text interactive elements
- [ ] Use `setAriaLabel()`, `setAriaLabelledBy()` on Vaadin components
- [ ] Set `helperText` and `errorMessage` on form fields
- [ ] Use `@PageTitle` annotation on all `@Route` views
- [ ] Ensure `Grid` columns have accessible headers
- [ ] Use `Dialog.setAriaLabel()` on all dialogs
- [ ] Set `Notification` to appropriate ARIA roles (`alert` for errors, `status` for info)
- [ ] Ensure custom components expose proper ARIA roles and properties
- [ ] Test all views with keyboard-only navigation
- [ ] Verify focus management in dynamic content (dialogs, overlays)

#### ✓ Performance

- [ ] Use virtual scrolling for large lists (`Grid`, `VirtualList`)
- [ ] Implement pagination where appropriate
- [ ] Minimize server round trips with batching
- [ ] Use `UI.access()` for background thread updates
- [ ] Leverage browser caching for static resources

### WCAG 2.1 AAA Accessibility Compliance

AAA conformance requires meeting **all** Level A, AA, and AAA success criteria (78 total).
The four principles: **Perceivable, Operable, Understandable, Robust (POUR)**.

#### ✓ Principle 1: Perceivable

##### 1.1 Text Alternatives (Level A)

- [ ] **1.1.1 Non-text Content**: All non-text content has text alternatives (`alt`, `aria-label`, `aria-labelledby`)
- [ ] Decorative images use `alt=""` or CSS backgrounds
- [ ] Form controls have accessible names via `<label>` or ARIA
- [ ] CAPTCHAs provide alternative forms (audio, logic-based)

##### 1.2 Time-Based Media (Levels A / AA / AAA)

- [ ] **1.2.1** Prerecorded audio-only: provide text transcript (Level A)
- [ ] **1.2.2** Prerecorded video with audio: provide synchronized captions (Level A)
- [ ] **1.2.3** Prerecorded video: provide audio description or media alternative (Level A)
- [ ] **1.2.4** Live audio: provide real-time captions (Level AA)
- [ ] **1.2.5** Prerecorded video: provide audio descriptions (Level AA)
- [ ] **1.2.6** Prerecorded audio: provide sign language interpretation (Level AAA)
- [ ] **1.2.7** Prerecorded video: provide extended audio descriptions when pauses are insufficient (Level AAA)
- [ ] **1.2.8** Prerecorded synchronized media: provide full text alternative document (Level AAA)
- [ ] **1.2.9** Live audio-only: provide real-time text alternative (Level AAA)

##### 1.3 Adaptable (Levels A / AA / AAA)

- [ ] **1.3.1 Info and Relationships**: Structure and relationships are programmatically determinable (Level A)
- [ ] **1.3.2 Meaningful Sequence**: Reading order is programmatically determinable (Level A)
- [ ] **1.3.3 Sensory Characteristics**: Instructions do not rely solely on shape, color, size, or sound (Level A)
- [ ] **1.3.4 Orientation**: Content works in both portrait and landscape (Level AA)
- [ ] **1.3.5 Identify Input Purpose**: Form fields use `autocomplete` attributes for standard inputs (Level AA)
- [ ] **1.3.6 Identify Purpose**: UI components, icons, and regions have programmatically determinable purposes via ARIA landmarks and semantic HTML (Level AAA)

##### 1.4 Distinguishable (Levels A / AA / AAA)

- [ ] **1.4.1 Use of Color**: Color is not the sole means of conveying information (Level A)
- [ ] **1.4.2 Audio Control**: Auto-playing audio > 3 seconds has pause/stop/volume controls (Level A)
- [ ] **1.4.3 Contrast (Minimum)**: Text has 4.5:1 contrast ratio; large text has 3:1 (Level AA)
- [ ] **1.4.4 Resize Text**: Text resizes up to 200% without loss of content (Level AA)
- [ ] **1.4.5 Images of Text**: Use real text instead of images of text (Level AA)
- [ ] **1.4.6 Contrast (Enhanced)**: Text has **7:1** contrast ratio; large text has **4.5:1** (Level AAA)
- [ ] **1.4.7 Low or No Background Audio**: Speech audio has background ≥20dB quieter, or background can be disabled (Level AAA)
- [ ] **1.4.8 Visual Presentation**: Text blocks allow user-selected colors, max 80 characters per line, no justified text, line spacing ≥1.5x, resizable to 200% without horizontal scrolling (Level AAA)
- [ ] **1.4.9 Images of Text (No Exception)**: No images of text except pure decoration or essential presentations (Level AAA)
- [ ] **1.4.10 Reflow**: Content works at 320px width without horizontal scrolling (Level AA)
- [ ] **1.4.11 Non-text Contrast**: UI components and graphics have 3:1 contrast ratio (Level AA)
- [ ] **1.4.12 Text Spacing**: Content supports line-height 1.5x, paragraph spacing 2x, letter-spacing 0.12em, word-spacing 0.16em (Level AA)
- [ ] **1.4.13 Content on Hover/Focus**: Hover/focus content is dismissible, hoverable, and persistent (Level AA)

#### ✓ Principle 2: Operable

##### 2.1 Keyboard Accessible (Levels A / AA / AAA)

- [ ] **2.1.1 Keyboard**: All functionality is operable via keyboard (Level A)
- [ ] **2.1.2 No Keyboard Trap**: Focus can always be moved away using the keyboard (Level A)
- [ ] **2.1.3 Keyboard (No Exception)**: All functionality operable via keyboard, no exceptions (Level AAA)
- [ ] **2.1.4 Character Key Shortcuts**: Single-character shortcuts can be turned off, remapped, or are focus-dependent (Level A)

##### 2.2 Enough Time (Levels A / AA / AAA)

- [ ] **2.2.1 Timing Adjustable**: Time limits can be turned off, adjusted (10x), or extended (Level A)
- [ ] **2.2.2 Pause, Stop, Hide**: Moving/blinking content >5s has pause/stop controls (Level A)
- [ ] **2.2.3 No Timing**: Timing is not essential to any activity (Level AAA)
- [ ] **2.2.4 Interruptions**: Non-emergency interruptions can be postponed or suppressed (Level AAA)
- [ ] **2.2.5 Re-authenticating**: Data is preserved through session re-authentication (Level AAA)
- [ ] **2.2.6 Timeouts**: Users are warned about inactivity timeouts that cause data loss (Level AAA)

##### 2.3 Seizures and Physical Reactions (Levels A / AAA)

- [ ] **2.3.1 Three Flashes or Below Threshold**: No content flashes > 3 times/second (Level A)
- [ ] **2.3.2 Three Flashes**: No content flashes > 3 times/second, no exceptions (Level AAA)
- [ ] **2.3.3 Animation from Interactions**: Motion animation can be disabled (Level AAA)

##### 2.4 Navigable (Levels A / AA / AAA)

- [ ] **2.4.1 Bypass Blocks**: Mechanism to skip repeated content blocks (Level A)
- [ ] **2.4.2 Page Titled**: Pages have descriptive titles (Level A)
- [ ] **2.4.3 Focus Order**: Focus order preserves meaning and operability (Level A)
- [ ] **2.4.4 Link Purpose (In Context)**: Link purpose is clear from text or context (Level A)
- [ ] **2.4.5 Multiple Ways**: Multiple methods to locate pages (search, sitemap, nav) (Level AA)
- [ ] **2.4.6 Headings and Labels**: Headings and labels describe topic or purpose (Level AA)
- [ ] **2.4.7 Focus Visible**: Keyboard focus indicator is visible (Level AA)
- [ ] **2.4.8 Location**: User's location within the site is communicated (breadcrumbs, highlighted nav) (Level AAA)
- [ ] **2.4.9 Link Purpose (Link Only)**: Link purpose is identifiable from link text alone (Level AAA)
- [ ] **2.4.10 Section Headings**: Content is organized with section headings (Level AAA)

##### 2.5 Input Modalities (Levels A / AAA)

- [ ] **2.5.1 Pointer Gestures**: Multipoint/path-based gestures have single-pointer alternatives (Level A)
- [ ] **2.5.2 Pointer Cancellation**: Actions complete on up-event, not down-event (Level A)
- [ ] **2.5.3 Label in Name**: Visible label text is included in the accessible name (Level A)
- [ ] **2.5.4 Motion Actuation**: Motion-triggered actions have alternative controls (Level A)
- [ ] **2.5.5 Target Size**: Interactive targets are at least **44x44 CSS pixels** (Level AAA)
- [ ] **2.5.6 Concurrent Input Mechanisms**: No restriction on input modality (keyboard, mouse, touch all work) (Level AAA)

#### ✓ Principle 3: Understandable

##### 3.1 Readable (Levels A / AA / AAA)

- [ ] **3.1.1 Language of Page**: Page language is set via `lang` attribute (Level A)
- [ ] **3.1.2 Language of Parts**: Language changes within content are marked (Level AA)
- [ ] **3.1.3 Unusual Words**: Definitions are provided for jargon, idioms, and unusual terms (Level AAA)
- [ ] **3.1.4 Abbreviations**: Expansions are provided for abbreviations (Level AAA)
- [ ] **3.1.5 Reading Level**: Content does not exceed lower secondary reading level, or simpler alternatives are provided (Level AAA)
- [ ] **3.1.6 Pronunciation**: Pronunciation guidance is provided where meaning depends on it (Level AAA)

##### 3.2 Predictable (Levels A / AA / AAA)

- [ ] **3.2.1 On Focus**: Focus does not trigger unexpected context changes (Level A)
- [ ] **3.2.2 On Input**: Input changes do not auto-submit without warning (Level A)
- [ ] **3.2.3 Consistent Navigation**: Navigation is consistent across pages (Level AA)
- [ ] **3.2.4 Consistent Identification**: Same-function components are identified consistently (Level AA)
- [ ] **3.2.5 Change on Request**: Context changes occur only on explicit user request (Level AAA)

##### 3.3 Input Assistance (Levels A / AA / AAA)

- [ ] **3.3.1 Error Identification**: Errors are detected and described in text (Level A)
- [ ] **3.3.2 Labels or Instructions**: Input fields have labels or instructions (Level A)
- [ ] **3.3.3 Error Suggestion**: Error messages suggest corrections (Level AA)
- [ ] **3.3.4 Error Prevention (Legal, Financial, Data)**: Submissions are reversible, verified, or confirmable (Level AA)
- [ ] **3.3.5 Help**: Context-sensitive help is available for input fields (Level AAA)
- [ ] **3.3.6 Error Prevention (All)**: **All** form submissions are reversible, verified, or confirmable (Level AAA)

#### ✓ Principle 4: Robust

##### 4.1 Compatible (Levels A / AA)

- [ ] **4.1.1 Parsing**: HTML markup is well-formed with unique IDs, correct nesting, no duplicate attributes (Level A)
- [ ] **4.1.2 Name, Role, Value**: All UI components have programmatically determinable name, role, states, and properties (Level A)
- [ ] **4.1.3 Status Messages**: Status messages use `aria-live` regions so screen readers announce them without focus change (Level AA)

---

## Best Practices Guidelines

### Architecture & Design

#### Layered Architecture

**Rules:**

- Views should not directly access repositories
- Services should be stateless and transactional
- Keep domain logic in the domain model when possible
- Use DTOs for API boundaries to avoid exposing entities

### Code Style & Conventions

#### Naming Conventions

- Classes: `PascalCase` (CustomerService, OrderRepository)
- Methods: `camelCase` (findCustomerById, calculateTotal)
- Constants: `UPPER_SNAKE_CASE` (MAX_RETRY_ATTEMPTS, DEFAULT_TIMEOUT)
- Packages: `lowercase` (com.company.service)
- Test classes: `*Test` (CustomerServiceTest)

#### Method Design

- [ ] Methods should do one thing well (Single Responsibility)
- [ ] Limit method parameters (max 3-4, use objects for more)
- [ ] Return early to reduce nesting
- [ ] Use meaningful parameter names
- [ ] Avoid side effects in getter methods

#### Exception Handling

- [ ] Catch specific exceptions, not generic `Exception`
- [ ] Use try-with-resources for `AutoCloseable` resources
- [ ] Don't swallow exceptions without logging
- [ ] Create custom exceptions for domain-specific errors
- [ ] Use `@ApplicationException(rollback = true)` for transactional rollback

#### Null Safety

- [ ] Use `Optional` for return types that may be absent
- [ ] Never return `null` for collections (return empty collections)
- [ ] Use `Objects.requireNonNull()` for parameter validation
- [ ] Consider `@NonNull` annotations for documentation
- [ ] Avoid `Optional` as method parameters or fields

### Testing Standards

#### Unit Testing

- [ ] Use JUnit 5 with proper lifecycle annotations
- [ ] Follow AAA pattern: Arrange, Act, Assert
- [ ] Use meaningful test method names: `shouldReturnCustomerWhenIdExists`
- [ ] Mock dependencies with Mockito
- [ ] Aim for high coverage on business logic (80%+)

#### Integration Testing

- [ ] Use Arquillian or TestContainers for Jakarta EE integration tests
- [ ] Test actual database interactions
- [ ] Verify transaction boundaries
- [ ] Test REST endpoints with REST Assured

#### Accessibility Testing (WCAG 2.1 AAA)

- [ ] **Automated Testing**: Run axe-core or pa11y in CI/CD pipeline (catches ~30-40% of issues)
- [ ] **Keyboard Testing**: Navigate all views using only keyboard (Tab, Shift+Tab, Enter, Space, Escape, Arrow keys)
- [ ] **Screen Reader Testing**: Test with NVDA (Windows), VoiceOver (macOS), or JAWS
- [ ] **Visual Testing**: Verify at 200% zoom, 320px viewport width, and with high-contrast mode
- [ ] **Color Contrast**: Validate all text meets 7:1 ratio using contrast analyzer tools
- [ ] **Focus Management**: Verify focus is visible, logical in order, and properly managed in dialogs/overlays
- [ ] **Form Validation**: Verify errors are announced, descriptive, and suggest corrections
- [ ] **Motion/Animation**: Test with `prefers-reduced-motion: reduce` enabled
- [ ] **User Testing**: Validate with assistive technology users when possible

### Security Best Practices

#### Authentication & Authorization

- [ ] Use Jakarta Security with identity stores
- [ ] Implement role-based access control (RBAC)
- [ ] Secure REST endpoints with `@RolesAllowed`
- [ ] Secure Vaadin views with security annotations
- [ ] Use HTTPS in production

#### Input Validation

- [ ] Always validate at API boundaries
- [ ] Use Bean Validation annotations
- [ ] Sanitize inputs to prevent injection attacks
- [ ] Validate file uploads (type, size, content)
- [ ] Use parameterized queries to prevent SQL injection

### Performance Optimization

#### Database Access

- [ ] Use connection pooling (HikariCP, Agroal)
- [ ] Enable second-level cache for read-heavy entities
- [ ] Use database indexes strategically
- [ ] Implement pagination for large result sets
- [ ] Monitor and optimize slow queries

#### Caching Strategy

- [ ] Cache immutable data at application scope
- [ ] Use CDI producers for cached resources
- [ ] Implement cache invalidation strategies
- [ ] Consider Jakarta Cache API for distributed caching

#### Vaadin Performance

- [ ] Use push only when necessary (avoid polling)
- [ ] Implement lazy loading for grids
- [ ] Minimize component tree depth
- [ ] Use CSS for styling, not Java when possible

---

## Dependency Management Guidelines

### Maven Configuration

#### Dependency Hierarchy

#### Essential Dependencies Only

### Dependency Review Checklist

- [ ] Is this dependency truly necessary?
- [ ] Does Jakarta EE provide equivalent functionality?
- [ ] Is this the most recent stable version?
- [ ] Are there any CVE vulnerabilities? (check with OWASP Dependency-Check)
- [ ] Does it introduce transitive dependency conflicts?
- [ ] Is the license compatible with our project?
- [ ] Is it actively maintained?

### Avoiding Common Pitfalls

- [ ] Don't mix Spring and Jakarta EE annotations
- [ ] Avoid including `jakarta.jakartaee-api` with `compile` scope
- [ ] Don't add Apache Commons when Java standard library suffices
- [ ] Avoid Lombok with records (records already minimize boilerplate)
- [ ] Don't include multiple logging frameworks

---

## Code Review Process

### Pre-Review Checklist

- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] No obvious security vulnerabilities
- [ ] Follows project coding standards
- [ ] Updated documentation if needed
- [ ] No commented-out code blocks
- [ ] No debug print statements

### Review Focus Areas

#### 1. Functionality

- Does the code do what it's supposed to do?
- Are edge cases handled?
- Is error handling appropriate?

#### 2. Design

- Is the code in the right layer?
- Are abstractions appropriate?
- Is there proper separation of concerns?
- Can this be simplified?

#### 3. Readability

- Can you understand the code without extensive comments?
- Are names descriptive and consistent?
- Is the code formatted consistently?

#### 4. Performance

- Are there obvious performance issues?
- Is database access optimized?
- Are collections used appropriately?

#### 5. Security

- Is user input validated?
- Are sensitive operations authorized?
- Is data properly sanitized?

#### 6. Testing

- Is test coverage adequate?
- Are tests meaningful and maintainable?
- Do tests verify behavior, not implementation?

#### 7. Accessibility (WCAG 2.1 AAA)

- Do all interactive elements have accessible names?
- Is the content navigable by keyboard alone?
- Does the color contrast meet 7:1 (enhanced) ratio for text?
- Are form fields labeled with helper text and clear error messages?
- Do status messages use ARIA live regions?
- Is focus managed correctly for dynamic content (dialogs, notifications)?
- Are skip links and landmark regions present?
- Is content structured with proper heading hierarchy?
- Do images and non-text content have appropriate text alternatives?
- Can users control timing, interruptions, and animations?
- Are interactive targets at least 44x44 CSS pixels?
- Does the UI work at 200% zoom and 320px viewport width?

### Providing Feedback

- Be specific and constructive
- Explain the "why" behind suggestions
- Distinguish between required changes and suggestions
- Acknowledge good code and improvements
- Focus on code, not the person

---

## Common Anti-Patterns to Avoid

### Java-Specific

- **God Objects**: Classes that do too much (>500 lines, >20 methods)
- **Primitive Obsession**: Using primitives instead of value objects
- **String Typing**: Using strings for IDs instead of typed identifiers
- **Mutable Data Carriers**: Using mutable POJOs when records suffice

### Jakarta EE-Specific

- **Repository Fat**: Putting business logic in repositories
- **Transaction Spanning**: Long-running transactions that lock resources
- **Entity Exposure**: Returning JPA entities directly from REST endpoints
- **Cyclic Dependencies**: Services depending on each other

### Vaadin-Specific

- **View Logic**: Complex business logic in view classes
- **Stateful Views**: Maintaining state in UI components incorrectly
- **Synchronization Issues**: Accessing UI from background threads without `UI.access()`
- **Memory Leaks**: Not detaching listeners or observers

### Lumo Styling Anti-Patterns

- **Hard-Coded Colors**: Using `#hex` or `rgb()` values instead of `--lumo-*-color` properties
- **Hard-Coded Sizes**: Using `px`/`rem` literals instead of `--lumo-size-*` and `--lumo-space-*` tokens
- **Hard-Coded Typography**: Setting font-family or font-size directly instead of `--lumo-font-family` and `--lumo-font-size-*`
- **Inline Styles Over Utilities**: Using `component.getStyle().set(...)` when `LumoUtility` classes exist for the same purpose
- **Raw CSS Class Strings**: Writing `addClassName("p-m")` instead of `addClassName(LumoUtility.Padding.MEDIUM)`
- **Ignoring Dark Mode**: Styling with fixed colors that break in Lumo dark variant
- **Deep Component Selectors**: Writing complex CSS selectors to override Vaadin component internals instead of using `--lumo-*` property overrides or theme variants
- **Mixing Themes**: Applying custom CSS frameworks (Bootstrap, Tailwind) alongside Lumo, causing conflicts
- **Ignoring Compact Mode**: Layouts that break when compact mode is enabled because hard-coded sizes don't scale

### Accessibility Anti-Patterns

- **Missing Alt Text**: Images and icons without `alt` or `aria-label`
- **Color-Only Information**: Using color alone to convey status, errors, or meaning
- **Keyboard Traps**: Focus enters a component (dialog, menu) and cannot exit via keyboard
- **Auto-Context Changes**: Navigation or content changes triggered by focus or input without user action
- **Missing Form Labels**: Input fields without associated `<label>` or `aria-label`
- **Generic Link Text**: Links labeled "Click here", "Read more" without context
- **Inaccessible Custom Components**: Custom widgets that lack ARIA roles, states, and keyboard support
- **Invisible Focus**: Custom CSS that removes or hides focus indicators (`outline: none` without replacement)
- **Images of Text**: Using images to render text content instead of styled HTML text
- **Auto-Playing Media**: Audio or video that plays automatically without user controls
- **Fixed Font Sizes**: Using `px` for font sizes preventing user zoom/scaling
- **Missing Landmarks**: Pages without semantic regions (`<main>`, `<nav>`, `<header>`, `<footer>`)
- **Missing Page Titles**: Views without `@PageTitle` or descriptive titles
- **Timeout Without Warning**: Session or form timeouts that discard data without notice
- **Justified Text**: Using `text-align: justify` which creates uneven spacing for dyslexic users
- **Low Contrast**: Text below 7:1 contrast ratio (AAA) or 4.5:1 for large text

---

## Metrics & Code Quality

### Quality Metrics

- **Cyclomatic Complexity**: Keep methods under 10
- **Code Coverage**: Aim for 80%+ on business logic
- **Technical Debt Ratio**: Monitor and address regularly
- **Maintainability Index**: Keep above 20

### Tools Integration

- **Static Analysis**: SonarQube, SpotBugs
- **Dependency Check**: OWASP Dependency-Check
- **Code Formatting**: Maven Checkstyle, Spotless
- **Test Coverage**: JaCoCo
- **Accessibility Audit**: axe-core, pa11y, WAVE, Lighthouse
- **Contrast Checking**: WebAIM Contrast Checker, Colour Contrast Analyser
- **Screen Readers**: NVDA, VoiceOver, JAWS
- **HTML Validation**: W3C Markup Validator

### Build Configuration

---

## Resources & References

### Official Documentation

- Java 21 Features: https://openjdk.org/projects/jdk/21/
- Jakarta EE 10: https://jakarta.ee/specifications/platform/10/
- Vaadin 25: https://vaadin.com/docs/latest/
- CDI Specification: https://jakarta.ee/specifications/cdi/
- JPA Specification: https://jakarta.ee/specifications/persistence/

### Vaadin 25 Lumo Theme Resources

- Lumo Theme Reference: https://vaadin.com/docs/latest/styling/themes/lumo
- Lumo Style Properties: https://vaadin.com/docs/latest/styling/lumo/lumo-style-properties
- Lumo Color Properties: https://vaadin.com/docs/latest/styling/lumo/lumo-style-properties/color
- Lumo Typography Properties: https://vaadin.com/docs/latest/styling/lumo/lumo-style-properties/typography
- Lumo Size & Space Properties: https://vaadin.com/docs/latest/styling/lumo/lumo-style-properties/size-space
- Lumo Utility Classes: https://vaadin.com/docs/latest/styling/lumo/utility-classes
- Vaadin Styling Guide: https://vaadin.com/docs/latest/styling

### Accessibility Standards & Resources

- WCAG 2.1 Specification: https://www.w3.org/TR/WCAG21/
- WCAG 2.1 Quick Reference: https://www.w3.org/WAI/WCAG21/quickref/
- WAI-ARIA 1.2 Specification: https://www.w3.org/TR/wai-aria-1.2/
- WAI-ARIA Authoring Practices: https://www.w3.org/WAI/ARIA/apg/
- Vaadin Accessibility Guide: https://vaadin.com/docs/latest/accessibility
- WebAIM Contrast Checker: https://webaim.org/resources/contrastchecker/
- axe-core (Deque): https://github.com/dequelabs/axe-core
- pa11y: https://pa11y.org/

### Books & Learning

- "Effective Java" by Joshua Bloch (4th Edition when available)
- "Java Concurrency in Practice" by Brian Goetz
- "Domain-Driven Design" by Eric Evans
- "Clean Architecture" by Robert C. Martin

### Community Standards

- Java Code Conventions: https://www.oracle.com/java/technologies/javase/codeconventions-contents.html
- Google Java Style Guide: https://google.github.io/styleguide/javaguide.html

---

## Version History

| Version | Date       | Changes                                                                                  |
| ------- | ---------- | ---------------------------------------------------------------------------------------- |
| 1.0     | 2026-01-31 | Initial version for Java 21, Jakarta EE 10, Vaadin 25                                    |
| 1.1     | 2026-01-31 | Added WCAG 2.1 AAA accessibility guidelines across all sections                          |
| 1.2     | 2026-01-31 | Added Vaadin 25 Lumo theme styling guidelines, properties reference, and utility classes |
| 1.3     | 2026-01-31 | Moved code examples to CODING-GUIDELINES.md; CLAUDE.md now contains guidelines only      |

---

## Contributing to This Guide

This guide should be treated as a living document. When you identify new patterns, anti-patterns, or best practices:

1. Discuss with the team
2. Update this document with clear examples
3. Document the rationale behind changes
4. Update the version history

---

**Remember**: The goal is not perfection, but continuous improvement. These guidelines help maintain consistency, quality, and maintainability across the codebase while embracing modern Java capabilities and standards-based development.
