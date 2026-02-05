# Static Assets Quick Reference

## Directory Structure

```
META-INF/resources/
├── styles/              ← CSS Stylesheets
├── images/              ← Image files
├── js/                  ← JavaScript files
├── styles.css           ← Lumo brand variables
└── styles-aura.css      ← Aura brand variables
```

## Adding New Files

### 📄 CSS Stylesheet

**File**: `src/main/resources/META-INF/resources/styles/my-styles.css`

**In Java Component**:

```java
@StyleSheet("styles/my-styles.css")
public class MyView extends VerticalLayout { }
```

**Or Global** (in AppShell):

```java
@StyleSheet("styles/utilities.css")
public class AppShell implements AppShellConfigurator { }
```

### 🖼️ Image

**File**: `src/main/resources/META-INF/resources/images/logo.png`

**In Java**:

```java
Image img = new Image("images/logo.png", "Logo");
```

**In CSS**:

```css
.logo {
  background-image: url("images/logo.png");
  background-size: 200px;
}
```

### ⚙️ JavaScript

**File**: `src/main/resources/META-INF/resources/js/script.js`

**In Java**:

```java
@JavaScript("js/script.js")
public class MyView extends VerticalLayout { }
```

**Execute from Java**:

```java
UI.getCurrent().getPage().executeJs("console.log('Hello');");
```

## CSS Variables (Always Use These!)

```css
/* Colors */
var(--lumo-base-color)
var(--lumo-primary-color)
var(--lumo-success-color)
var(--lumo-warning-color)
var(--lumo-error-color)
var(--lumo-body-text-color)

/* Spacing */
var(--lumo-space-xs)    /* 4px */
var(--lumo-space-s)     /* 8px */
var(--lumo-space-m)     /* 16px */
var(--lumo-space-l)     /* 24px */

/* Brand Colors */
var(--brand-teal-80)
var(--brand-sage-80)
var(--brand-gold-60)
var(--brand-deep-navy-100)
```

## File Paths

| What     | Where             | URL                 |
| -------- | ----------------- | ------------------- |
| CSS file | `styles/file.css` | `"styles/file.css"` |
| Image    | `images/file.png` | `"images/file.png"` |
| JS file  | `js/file.js`      | `"js/file.js"`      |

## Naming Convention (BEM)

```
.block
.block__element
.block--modifier
.block--modifier__element
```

**Example**:

```css
.card {
}
.card__header {
}
.card__title {
}
.card--primary {
}
.card--primary__header {
}
```

## Common Tasks

### Add component-specific CSS

```java
@StyleSheet("styles/components.css")
public class MyComponent extends Div { }
```

### Add global utilities

```java
@StyleSheet("styles/utilities.css")
public class AppShell implements AppShellConfigurator { }
```

### Use theme-aware colors

```css
.my-element {
  background-color: var(--lumo-base-color);
  color: var(--lumo-body-text-color);
}
```

### Avoid after theme switch

```css
/* ❌ DON'T */
background-color: #ffffff;

/* ✅ DO */
background-color: var(--lumo-base-color);
```

## File Size Limits

- Images: Keep under 500KB (compress first)
- Stylesheets: No limit, but organize into multiple files
- JavaScript: Keep performance in mind

## Browser Support

- Modern browsers (Chrome, Firefox, Safari, Edge)
- CSS Variables: Supported in all modern browsers
- CSS Grid/Flexbox: Fully supported
- ES6 JavaScript: Fully supported

## Resources

- Full Guide: [static-assets-guide.md](static-assets-guide.md)
- Brand Variables: [brand-variables-guide.md](brand-variables-guide.md)
- Theme Switching: [themswitch-changes.md](themswitch-changes.md)

## Template Files

- CSS Template: `styles/components.css` ← Copy and customize
