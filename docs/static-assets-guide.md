# Static Assets & Stylesheet Management Guide

## Overview

This guide explains how to organize, add, and reference CSS stylesheets, JavaScript files, and images in your Vaadin 25 application while maintaining consistency with dynamic theme switching.

---

## Project Asset Structure

```
src/main/resources/META-INF/resources/
├── images/
│   ├── background-main-dark.jpg
│   ├── background-main.svg
│   └── logo.png              ← Add your images here
├── styles/
│   ├── app-layout.css        ← Main application layout
│   ├── components.css        ← Component-specific styles
│   ├── forms.css             ← Form styling
│   ├── tables.css            ← Table styling
│   └── utilities.css         ← Utility classes
├── js/
│   ├── custom-interactions.js ← Custom JavaScript
│   └── analytics.js          ← Analytics code
├── styles.css               ← Lumo theme brand variables
├── styles-aura.css          ← Aura theme brand variables
└── index.html              ← Static HTML (if needed)

src/main/frontend/
└── themes/my-theme/        ← For Vaadin design time theming (optional)
```

---

## Where to Add Files

### 📁 CSS Stylesheets

**Location**: `src/main/resources/META-INF/resources/styles/`

**Recommended Structure**:

```
styles/
├── app-layout.css          ← [EXISTING] Application layout
├── components.css          ← NEW: Custom component styles
├── forms.css              ← NEW: Form-specific styles
├── tables.css             ← NEW: Data table styles
├── dialogs.css            ← NEW: Dialog/modal styles
├── notifications.css      ← NEW: Notification styles
├── utilities.css          ← NEW: Utility classes
├── responsive.css         ← NEW: Media queries
└── animations.css         ← NEW: Animations & transitions
```

### 🖼️ Images

**Location**: `src/main/resources/META-INF/resources/images/`

**Recommended Structure**:

```
images/
├── backgrounds/
│   ├── background-main-dark.jpg    ← [EXISTING]
│   ├── background-main.svg         ← [EXISTING]
│   └── pattern.png                 ← NEW
├── icons/
│   ├── logo.png                    ← NEW
│   ├── icon-user.svg               ← NEW
│   ├── icon-settings.svg           ← NEW
│   └── icon-menu.svg               ← NEW
├── logos/
│   ├── company-logo.png            ← NEW
│   └── company-logo-dark.png       ← NEW
└── assets/
    ├── hero-image.jpg              ← NEW
    └── banner.png                  ← NEW
```

**Image Guidelines**:

- Use SVG for icons and logos (scalable)
- Use PNG for graphics with transparency
- Use JPG for photographs and backgrounds
- Use WebP for modern browsers (with fallbacks)
- Keep file sizes optimized (compress before adding)

### 🔧 JavaScript Files

**Location**: `src/main/resources/META-INF/resources/js/`

**Recommended Structure**:

```
js/
├── custom-interactions.js   ← Custom element behaviors
├── analytics.js            ← Analytics tracking
├── utils.js                ← Utility functions
└── polyfills.js            ← Browser compatibility
```

---

## How to Add New Stylesheets

### Method 1: Add to Component with @StyleSheet

```java
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

@StyleSheet("styles/components.css")
@StyleSheet("styles/forms.css")
public class MyCustomView extends VerticalLayout {
    public MyCustomView() {
        // Your component code
    }
}
```

**Advantages**:

- ✅ Stylesheet loaded only when component is used
- ✅ Organized by component
- ✅ Easy to manage dependencies

**Usage**:

- Use for component-specific styles
- Use for view-specific styles
- Multiple @StyleSheet annotations supported

### Method 2: Add to AppShellConfigurator (Global)

```java
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.component.dependency.StyleSheet;

@StyleSheet("styles/utilities.css")
@StyleSheet("styles/responsive.css")
@StyleSheet("styles/animations.css")
public class AppShell implements AppShellConfigurator {
    // Global stylesheets loaded for entire application
}
```

**Advantages**:

- ✅ Available application-wide
- ✅ Loaded once at startup
- ✅ Good for utilities and global styles

### Method 3: Inline in HTML Head (via Application.java)

```java
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.ServiceInitEvent;
import org.springframework.stereotype.Component;

@Component
public class CustomUIInitListener implements UIInitListener {
    @Override
    public void uiInit(UIInitEvent event) {
        event.getUI().getPage().addStyleSheet("styles/custom.css");
    }
}
```

**Advantages**:

- ✅ Dynamic stylesheet loading
- ✅ Load based on conditions
- ✅ Combine with theme switching

---

## Adding Images

### In Java Components

```java
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Div;

// Method 1: Direct image component
Image logo = new Image("images/logos/company-logo.png", "Company Logo");
logo.setWidth("200px");
logo.setHeight("auto");

// Method 2: CSS background
Div heroSection = new Div();
heroSection.getStyle()
    .set("background-image", "url('images/backgrounds/hero-image.jpg')")
    .set("background-size", "cover")
    .set("background-position", "center")
    .set("height", "400px");

// Method 3: SVG Icon
Image icon = new Image("images/icons/icon-user.svg", "User Icon");
icon.setWidth("24px");
icon.setHeight("24px");
```

### In CSS

```css
/* Background images */
.hero-banner {
  background-image: url("images/backgrounds/hero-image.jpg");
  background-size: cover;
  background-position: center;
  height: 400px;
}

/* Icon backgrounds */
.icon-button {
  background-image: url("images/icons/icon-menu.svg");
  background-size: 24px 24px;
  background-repeat: no-repeat;
  background-position: center;
}

/* Logos */
.company-logo {
  background-image: url("images/logos/company-logo.png");
  background-size: contain;
  background-repeat: no-repeat;
  width: 200px;
  height: 80px;
}
```

---

## Adding JavaScript Files

### Global JavaScript

```java
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.page.AppShellConfigurator;

@JavaScript("js/utils.js")
@JavaScript("js/analytics.js")
public class AppShell implements AppShellConfigurator {
    // Loaded globally for all views
}
```

### Component-Specific JavaScript

```java
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

@JavaScript("js/custom-interactions.js")
public class AdvancedView extends VerticalLayout {
    // JavaScript loaded when this view is used
}
```

### Execute JavaScript from Java

```java
import com.vaadin.flow.component.UI;

// Execute JavaScript in browser
UI.getCurrent().getPage().executeJs("console.log('Hello from Vaadin!');");

// Execute and get result
UI.getCurrent().getPage().executeJs(
    "return document.title;"
).then(String.class, title -> {
    System.out.println("Page title: " + title);
});
```

---

## Theme-Aware CSS Guidelines

### ✅ DO: Use CSS Variables

```css
/* Good - Respects theme switching */
.custom-card {
  background-color: var(--lumo-base-color);
  border: 1px solid var(--lumo-divider-color);
  color: var(--lumo-body-text-color);
}

.primary-button {
  background-color: var(--lumo-primary-color);
  color: var(--lumo-primary-contrast-color);
}

.success-badge {
  background-color: var(--brand-sage-80);
  color: var(--brand-sage-10);
}
```

### ❌ DON'T: Hardcode Colors

```css
/* Bad - Won't change on theme switch */
.custom-card {
  background-color: #ffffff;
  border: 1px solid #e0e0e0;
  color: #333333;
}
```

### Available CSS Variables

**Lumo Variables**:

```css
/* Colors */
--lumo-base-color
--lumo-primary-color
--lumo-success-color
--lumo-warning-color
--lumo-error-color

/* Contrast */
--lumo-primary-contrast-color
--lumo-primary-text-color
--lumo-body-text-color
--lumo-secondary-text-color

/* Spacing */
--lumo-space-xs  /* 4px */
--lumo-space-s   /* 8px */
--lumo-space-m   /* 16px */
--lumo-space-l   /* 24px */
--lumo-space-xl  /* 32px */

/* Typography */
--lumo-font-size-xs
--lumo-font-size-s
--lumo-font-size-m
--lumo-font-size-l
--lumo-font-size-xl
--lumo-font-family

/* Borders */
--lumo-border-radius-s
--lumo-border-radius-m
--lumo-border-radius-l
```

**Brand Variables**:

```css
--brand-deep-navy-100 to --brand-deep-navy-10
--brand-teal-100 to --brand-teal-10
--brand-sage-100 to --brand-sage-10
--brand-gold-100 to --brand-gold-10
```

---

## Complete Example: Adding New Component Styles

### Step 1: Create CSS File

**File**: `src/main/resources/META-INF/resources/styles/my-components.css`

```css
/**
 * Custom Component Styles
 * Theme-aware styles using Lumo variables
 */

/* Card Component */
.my-card {
  background-color: var(--lumo-base-color);
  border: 1px solid var(--lumo-divider-color);
  border-radius: var(--lumo-border-radius-m);
  padding: var(--lumo-space-m);
  box-shadow: 0 2px 8px var(--lumo-shade-10pct);
  transition: all 0.2s ease;
}

.my-card:hover {
  box-shadow: 0 4px 16px var(--lumo-shade-20pct);
}

.my-card__title {
  color: var(--lumo-header-text-color);
  font-size: var(--lumo-font-size-l);
  font-weight: bold;
  margin-bottom: var(--lumo-space-s);
}

.my-card__body {
  color: var(--lumo-body-text-color);
  line-height: 1.5;
}

/* Primary Variant */
.my-card--primary {
  border-left: 4px solid var(--lumo-primary-color);
}

/* Success Variant */
.my-card--success {
  border-left: 4px solid var(--brand-sage-80);
}

/* Alert Component */
.my-alert {
  padding: var(--lumo-space-m);
  border-radius: var(--lumo-border-radius-m);
  display: flex;
  gap: var(--lumo-space-m);
  align-items: flex-start;
}

.my-alert--info {
  background-color: var(--lumo-primary-color-10pct);
  border-left: 3px solid var(--lumo-primary-color);
  color: var(--lumo-primary-text-color);
}

.my-alert--warning {
  background-color: var(--lumo-warning-color-10pct);
  border-left: 3px solid var(--lumo-warning-color);
  color: var(--lumo-warning-text-color);
}

.my-alert--error {
  background-color: var(--lumo-error-color-10pct);
  border-left: 3px solid var(--lumo-error-color);
  color: var(--lumo-error-text-color);
}
```

### Step 2: Create Java Component

**File**: `src/main/java/com/example/vaadin/components/MyCard.java`

```java
package com.example.vaadin.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.dependency.StyleSheet;

@StyleSheet("styles/my-components.css")
public class MyCard extends Div {
    public enum Variant {
        DEFAULT(""),
        PRIMARY("my-card--primary"),
        SUCCESS("my-card--success");

        private final String className;

        Variant(String className) {
            this.className = className;
        }
    }

    public MyCard(String title, String body) {
        addClassName("my-card");

        H3 titleElement = new H3(title);
        titleElement.addClassName("my-card__title");

        Paragraph bodyElement = new Paragraph(body);
        bodyElement.addClassName("my-card__body");

        add(titleElement, bodyElement);
    }

    public void setVariant(Variant variant) {
        removeClassName("my-card--primary");
        removeClassName("my-card--success");
        if (!variant.className.isEmpty()) {
            addClassName(variant.className);
        }
    }
}
```

### Step 3: Use in View

```java
import com.example.vaadin.components.MyCard;

public class MyView extends VerticalLayout {
    public MyView() {
        MyCard card1 = new MyCard("Welcome", "This is a default card");

        MyCard card2 = new MyCard("Important", "This is a primary card");
        card2.setVariant(MyCard.Variant.PRIMARY);

        MyCard card3 = new MyCard("Success", "Operation completed!");
        card3.setVariant(MyCard.Variant.SUCCESS);

        add(card1, card2, card3);
    }
}
```

---

## Best Practices

### 1. File Organization

- ✅ Group related styles in separate files (forms.css, tables.css, etc.)
- ✅ One main component class per CSS class
- ✅ Use BEM naming convention for modifiers (`.component--variant`)
- ✅ Keep related images in subdirectories

### 2. Naming Conventions

```css
/* Good: BEM convention */
.my-component
.my-component__title
.my-component__body
.my-component--primary
.my-component--large

/* Avoid: Generic names */
.big-red-box
.style1
.temp-styles
```

### 3. CSS Variables

```css
/* Good: Use variables for consistency */
padding: var(--lumo-space-m);
color: var(--lumo-body-text-color);
background: var(--lumo-base-color);

/* Bad: Hardcoded values */
padding: 16px;
color: #333;
background: #fff;
```

### 4. Image Optimization

```
- SVG: Use for icons, logos, diagrams
- PNG: For graphics with transparency
- JPG: For photographs
- WebP: For modern browsers (with PNG fallback)
```

**Size recommendations**:

- Icons: 24px, 32px, 48px
- Logos: Keep under 100KB
- Backgrounds: Compress to < 500KB
- Use online tools: TinyPNG, Squoosh.app

### 5. Responsive Design

```css
/* Mobile first approach */
.my-component {
  padding: var(--lumo-space-s);
}

@media (min-width: 768px) {
  .my-component {
    padding: var(--lumo-space-m);
  }
}

@media (min-width: 1024px) {
  .my-component {
    padding: var(--lumo-space-l);
  }
}
```

---

## Troubleshooting

### CSS not applying

- **Check**: File path is correct relative to `src/main/resources/META-INF/resources/`
- **Check**: @StyleSheet annotation is present on component or AppShell
- **Check**: No typos in class names
- **Solution**: Hard refresh browser (Ctrl+Shift+R or Cmd+Shift+R)

### Images not loading

- **Check**: File exists in `src/main/resources/META-INF/resources/images/`
- **Check**: Path is correct (relative to resources/)
- **Check**: File extension matches (case-sensitive on Linux)
- **Solution**: Check browser console for 404 errors

### CSS variables not working after theme switch

- **Check**: Using `var()` not hardcoded colors
- **Check**: Variable names match those in styles.css and styles-aura.css
- **Check**: Stylesheet is not being removed
- **Solution**: See brand-variables-guide.md for complete variable list

### JavaScript not executing

- **Check**: File exists in `src/main/resources/META-INF/resources/js/`
- **Check**: @JavaScript annotation is present
- **Check**: Syntax errors in JS file (check console)
- **Solution**: Use browser DevTools to debug

---

## Directory Structure Summary

```
src/main/resources/META-INF/resources/
├── styles/                          ← CSS Stylesheets
│   ├── app-layout.css
│   ├── components.css               ← NEW
│   ├── forms.css                    ← NEW
│   ├── tables.css                   ← NEW
│   └── utilities.css                ← NEW
├── images/                          ← All images
│   ├── backgrounds/
│   ├── icons/
│   ├── logos/
│   └── assets/
├── js/                              ← JavaScript files
│   ├── custom-interactions.js       ← NEW
│   └── analytics.js                 ← NEW
├── styles.css                       ← Lumo brand variables
└── styles-aura.css                  ← Aura brand variables
```

---

## Quick Reference

| Task                    | Location               | Method                         |
| ----------------------- | ---------------------- | ------------------------------ |
| Add CSS for component   | `styles/my-styles.css` | `@StyleSheet(...)`             |
| Add global CSS          | `styles/utilities.css` | `@StyleSheet(...)` on AppShell |
| Add image               | `images/subdir/`       | URL: `images/subdir/file.ext`  |
| Add JavaScript          | `js/script.js`         | `@JavaScript(...)`             |
| Override Lumo variables | `styles.css`           | CSS variables                  |
| Override Aura variables | `styles-aura.css`      | CSS variables                  |

---

## Related Documentation

- [Vaadin 25 StyleSheet Documentation](https://vaadin.com/docs/latest/)
- [CSS Variables Guide](brand-variables-guide.md)
- [Theme Switching Guide](themswitch-changes.md)
- [MDN CSS Custom Properties](https://developer.mozilla.org/en-US/docs/Web/CSS/--*)
