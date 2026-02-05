# Troubleshooting: Stylesheets & Background Images Not Working

## Common Issues & Solutions

### Issue 1: CSS File Not Loading

**Symptoms**:

- Styles not applied
- No errors in browser console
- Classes exist but have no styling

**Solutions**:

#### Check the @StyleSheet path

```java
// ✅ CORRECT - from META-INF/resources/
@StyleSheet("./styles/app-layout.css")
public class AppLayout extends VerticalLayout { }

// ❌ WRONG - missing ./ prefix
@StyleSheet("styles/app-layout.css")

// ❌ WRONG - absolute path
@StyleSheet("/styles/app-layout.css")
```

**File must exist at**:

```
src/main/resources/META-INF/resources/styles/app-layout.css
```

#### Verify file permissions

```bash
# Check if file exists and is readable
ls -la src/main/resources/META-INF/resources/styles/app-layout.css
```

#### Clear browser cache

- Hard refresh: `Ctrl+Shift+R` (Windows/Linux) or `Cmd+Shift+R` (Mac)
- Or clear browser cache manually
- Or use DevTools: Disable cache while DevTools is open

#### Rebuild project

```bash
mvn clean install
```

---

### Issue 2: Background Image Not Showing

**Symptoms**:

- Background color shows but no image
- 404 error in DevTools Network tab
- Path shows in CSS but image doesn't load

**Solutions**:

#### Check image path in CSS

```css
/* ✅ CORRECT - relative to styles/ folder */
.app-content {
  background-image: url("../images/background-main-dark.jpg");
}

/* ❌ WRONG - missing ../ */
background-image: url("images/background-main-dark.jpg");

/* ❌ WRONG - absolute path */
background-image: url("/images/background-main-dark.jpg");
```

**Image file must exist at**:

```
src/main/resources/META-INF/resources/images/background-main-dark.jpg
```

#### Verify image file

```bash
# Check if image exists
ls -la src/main/resources/META-INF/resources/images/background-main-dark.jpg

# Check file size (should not be 0)
du -h src/main/resources/META-INF/resources/images/background-main-dark.jpg
```

#### Check DevTools

1. Open Browser DevTools (F12)
2. Go to Network tab
3. Look for the image URL
4. If 404, path is wrong
5. If loaded but not showing, check CSS properties

#### Correct CSS properties

```css
.app-content {
  background-image: url("../images/background-main-dark.jpg");
  background-size: cover; /* ← Important */
  background-position: center; /* ← Important */
  background-repeat: no-repeat; /* ← Important */
  background-attachment: fixed; /* ← Optional but recommended */
  height: 100%; /* ← Important - needs explicit height */
}
```

---

### Issue 3: CSS Variables Not Working

**Symptoms**:

- Colors are white/transparent
- Text not visible
- Theme switch doesn't update styles

**Solutions**:

#### Check if theme CSS files are loaded

1. Open DevTools
2. Go to Elements tab
3. Look for `<link rel="stylesheet">` tags
4. Should see `styles.css` or `styles-aura.css`
5. Check its URL is correct

#### Check CSS variables are defined

```css
/* ✅ CORRECT - using variables */
.app-header {
  background-color: var(--lumo-primary-color);
  color: var(--lumo-primary-contrast-color);
}

/* ❌ WRONG - hardcoded color */
.app-header {
  background-color: #2d7a8a;
  color: #ffffff;
}
```

#### Make sure theme files exist

```bash
# Check theme files
ls -la src/main/resources/META-INF/resources/styles.css
ls -la src/main/resources/META-INF/resources/styles-aura.css
```

---

## Debug Steps

### Step 1: Check File Locations

```bash
# Verify all files exist
find src/main/resources/META-INF/resources -type f
```

Expected output:

```
src/main/resources/META-INF/resources/styles.css
src/main/resources/META-INF/resources/styles-aura.css
src/main/resources/META-INF/resources/styles/app-layout.css
src/main/resources/META-INF/resources/styles/components.css
src/main/resources/META-INF/resources/images/background-main-dark.jpg
```

### Step 2: Check Java Annotations

```java
@StyleSheet("./styles/app-layout.css")  // ← Correct path
public class AppLayout extends VerticalLayout {
    // Check: Should have @StyleSheet with ./ prefix
}
```

### Step 3: Inspect in Browser

```javascript
// Open DevTools Console and run:

// Check if stylesheet is loaded
document.styleSheets.forEach((sheet) => {
  if (sheet.href) console.log(sheet.href);
});

// Check if CSS variables are accessible
const styles = getComputedStyle(document.documentElement);
console.log(styles.getPropertyValue("--lumo-primary-color"));

// Check if element has class
const element = document.querySelector(".app-content");
console.log(element.className);

// Check computed styles
console.log(window.getComputedStyle(element).backgroundImage);
```

### Step 4: Build and Deploy

```bash
# Clean build to ensure resources are copied
mvn clean install

# If using Spring Boot:
mvn clean spring-boot:run
```

---

## Complete Working Example

### File: AppLayout.java

```java
@StyleSheet("./styles/app-layout.css")
public class AppLayout extends VerticalLayout {
    @Inject
    public AppLayout(SecurityService securityService) {
        // Setup code
    }
}
```

### File: styles/app-layout.css

```css
.app-content {
  background-image: url("../images/background-main-dark.jpg");
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  background-repeat: no-repeat;
  flex: 1;
  overflow: auto;
  position: relative;
}

.app-header {
  background: linear-gradient(
    135deg,
    var(--lumo-primary-color) 0%,
    var(--brand-teal-100) 100%
  );
  padding: var(--lumo-space-m);
}

.app-footer {
  background-color: var(--lumo-base-color);
  border-top: 1px solid var(--lumo-divider-color);
  padding: var(--lumo-space-m);
}
```

### File structure

```
src/main/resources/META-INF/resources/
├── styles/
│   └── app-layout.css          ← Path: ./styles/app-layout.css
├── images/
│   └── background-main-dark.jpg ← Path in CSS: ../images/background-main-dark.jpg
├── styles.css                  ← Lumo variables
└── styles-aura.css            ← Aura variables
```

---

## Path Reference Guide

| File Location           | Java @StyleSheet            | CSS Image Path              |
| ----------------------- | --------------------------- | --------------------------- |
| `styles/app-layout.css` | `"./styles/app-layout.css"` | N/A                         |
| `images/bg.jpg`         | N/A                         | `url('../images/bg.jpg')`   |
| `styles/components.css` | `"./styles/components.css"` | `url('../images/icon.svg')` |

---

## Testing Checklist

- [ ] File exists at path shown in error
- [ ] @StyleSheet path starts with `./`
- [ ] Image path in CSS uses `../images/`
- [ ] Browser cache cleared (Ctrl+Shift+R)
- [ ] Project rebuilt (mvn clean install)
- [ ] CSS variables match variable names
- [ ] No typos in class names
- [ ] DevTools shows stylesheet in Network tab
- [ ] DevTools shows correct background-image URL
- [ ] Server restarted after changes

---

## Still Not Working?

### Check Maven Build Output

```bash
mvn clean install 2>&1 | grep -i "error\|warning"
```

### Verify File Was Copied to Target

```bash
find target -name "app-layout.css"
find target -name "background-main-dark.jpg"
```

### Check Server Logs

Look for errors about missing resources or stylesheet loading issues.

### Reset Everything

```bash
# Complete clean and rebuild
mvn clean
rm -rf target/
mvn install

# Restart server and hard refresh browser
```

---

## Resources

- [Static Assets Guide](static-assets-guide.md)
- [Brand Variables Guide](brand-variables-guide.md)
- [Quick Reference](quick-reference.md)
