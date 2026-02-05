Vaadin 25 Best Approach for Dynamic Theme Switching
In Vaadin 25, themes are just stylesheets that can be imported with @StyleSheet.
This allows changing styles, color schemes, and themes at runtime using `page.addStyleSheet()` and the returned remover. New Releases
Key API methods:

```
java// Load a theme
var remover = page.addStyleSheet(Lumo.STYLESHEET);

// Switch themes
remover.remove();
page.addStyleSheet(Aura.STYLESHEET);

// Change color scheme
page.setColorScheme(ColorScheme.Value.DARK);
```

# Quick Start

1. Place CSS files in src/main/resources/META-INF/resources/
2. For static single-theme (simpler):

```java

@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles.css")
public class Application implements AppShellConfigurator { }
```

3. For dynamic switching (your use case):

- Don't use @StyleSheet annotations
- Use ThemeSwitcher utility class
- Add the VaadinServiceInitListener bean to restore themes on UI init

4. Switch themes anywhere:

```java

ThemeSwitcher.switchToAura();
ThemeSwitcher.switchToLumo();
ThemeSwitcher.setColorScheme(ColorScheme.DARK);
ThemeSwitcher.toggleColorScheme();

```

Both CSS files share the same brand color tokens `(--brand-* variables)`, ensuring visual consistency when switching between Lumo and Aura.
