package com.example.app.ui.theme;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.theme.lumo.Lumo;
import java.io.Serializable;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * ============================================================================= THEME SWITCHER
 * UTILITY FOR VAADIN 25
 * =============================================================================
 * <p>
 * This utility class provides dynamic theme switching between Lumo and Aura themes at runtime in
 * Vaadin 25 applications. It also handles color scheme (light/dark mode) switching.
 * <p>
 * Vaadin 25 Best Practices for Theme Switching: --------------------------------------------- In
 * Vaadin 25, themes are just stylesheets that can be loaded/unloaded dynamically using
 * Page.addStyleSheet() and the returned Registration.remove() method.
 * <p>
 * Key Features: - Switch between Lumo and Aura themes at runtime - Switch between light and dark
 * color schemes - Persist theme preference in session - Support for custom brand stylesheets per
 * theme
 * <p>
 * Usage Examples: ---------------
 * <p>
 * 1. Basic theme switching: ThemeSwitcher.switchToAura(); ThemeSwitcher.switchToLumo();
 * <p>
 * 2. Color scheme switching: ThemeSwitcher.setColorScheme(ColorScheme.DARK);
 * ThemeSwitcher.toggleColorScheme();
 * <p>
 * 3. Combined theme and color scheme: ThemeSwitcher.applyTheme(Theme.AURA, ColorScheme.DARK);
 * <p>
 * 4. In a settings view with toggle: Checkbox darkMode = new Checkbox("Dark Mode");
 * darkMode.addValueChangeListener(e -> ThemeSwitcher.setColorScheme(e.getValue() ? ColorScheme.DARK
 * : ColorScheme.LIGHT) );
 * <p>
 * Select<Theme> themeSelect = new Select<>(); themeSelect.setItems(Theme.values());
 * themeSelect.addValueChangeListener(e -> ThemeSwitcher.switchTheme(e.getValue()));
 *
 * @author Your Name
 * @version 1.0
 * @since Vaadin 25
 */
public final class ThemeSwitcher implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(ThemeSwitcher.class.getName());

  // Session attribute keys
  private static final String THEME_SESSION_KEY = "app.theme";
  private static final String COLOR_SCHEME_SESSION_KEY = "app.colorScheme";
  private static final String THEME_REMOVER_SESSION_KEY = "app.themeRemover";
  private static final String CUSTOM_STYLE_REMOVER_SESSION_KEY = "app.customStyleRemover";

  // Custom stylesheet paths (relative to META-INF/resources or webapp)
  private static final String LUMO_CUSTOM_STYLES = "styles.css";
  private static final String AURA_CUSTOM_STYLES = "styles-aura.css";

  /**
   * Available themes in the application.
   */
  public enum Theme {
    LUMO("Lumo", Lumo.STYLESHEET, LUMO_CUSTOM_STYLES),
    AURA("Aura", Aura.STYLESHEET, AURA_CUSTOM_STYLES);

    private final String displayName;
    private final String stylesheetPath;
    private final String customStylesPath;

    Theme(String displayName, String stylesheetPath, String customStylesPath) {
      this.displayName = displayName;
      this.stylesheetPath = stylesheetPath;
      this.customStylesPath = customStylesPath;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getStylesheetPath() {
      return stylesheetPath;
    }

    public String getCustomStylesPath() {
      return customStylesPath;
    }

    @Override
    public String toString() {
      return displayName;
    }
  }

  /**
   * Color scheme options.
   */
  public enum ColorScheme {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System (Auto)");

    private final String displayName;

    ColorScheme(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }

    @Override
    public String toString() {
      return displayName;
    }
  }

  // Private constructor to prevent instantiation
  private ThemeSwitcher() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  // =========================================================================
  // THEME SWITCHING METHODS
  // =========================================================================

  /**
   * Switches to the Lumo theme with the current color scheme.
   */
  public static void switchToLumo() {
    switchTheme(Theme.LUMO);
  }

  /**
   * Switches to the Aura theme with the current color scheme.
   */
  public static void switchToAura() {
    switchTheme(Theme.AURA);
  }

  /**
   * Switches to the specified theme while preserving the current color scheme.
   *
   * @param theme The theme to switch to
   */
  public static void switchTheme(Theme theme) {
    UI ui = UI.getCurrent();
    if (ui == null) {
      throw new IllegalStateException(
          "No UI available. Theme switching must be called from a UI context.");
    }

    Page page = ui.getPage();
    VaadinSession session = VaadinSession.getCurrent();

    // Remove existing theme stylesheet
    removeExistingTheme(session);

    // Add new theme stylesheet and store the remover
    var themeRemover = page.addStyleSheet(theme.getStylesheetPath());
    session.setAttribute(THEME_REMOVER_SESSION_KEY, (Runnable) themeRemover::remove);

    // Add custom brand styles for the theme
    var customStyleRemover = page.addStyleSheet(theme.getCustomStylesPath());
    session.setAttribute(CUSTOM_STYLE_REMOVER_SESSION_KEY, (Runnable) customStyleRemover::remove);

    // Store current theme in session
    session.setAttribute(THEME_SESSION_KEY, theme);

    // Log theme change (optional, for debugging)
    LOGGER.info("Theme switched to: " + theme.getDisplayName());
  }

  /**
   * Applies both theme and color scheme at once.
   *
   * @param theme       The theme to apply
   * @param colorScheme The color scheme to apply
   */
  public static void applyTheme(Theme theme, ColorScheme colorScheme) {
    switchTheme(theme);
    setColorScheme(colorScheme);
  }

  /**
   * Gets the currently active theme.
   *
   * @return The current theme, or LUMO as default
   */
  public static Theme getCurrentTheme() {
    VaadinSession session = VaadinSession.getCurrent();
    if (session == null) {
      return Theme.LUMO;
    }

    Theme theme = (Theme) session.getAttribute(THEME_SESSION_KEY);
    return theme != null ? theme : Theme.LUMO;
  }

  // =========================================================================
  // COLOR SCHEME METHODS
  // =========================================================================

  /**
   * Sets the color scheme (light/dark mode). This works with both Aura and Lumo themes.
   *
   * @param colorScheme The color scheme to apply
   */
  public static void setColorScheme(ColorScheme colorScheme) {
    VaadinSession session = VaadinSession.getCurrent();
    if (session == null) {
      throw new IllegalStateException(
          "No session available. Color scheme switching must be called from a Vaadin context.");
    }

    // Store in session for persistence
    session.setAttribute(COLOR_SCHEME_SESSION_KEY, colorScheme);

    // Log color scheme change (optional, for debugging)
    LOGGER.info("Color scheme set to: " + colorScheme.getDisplayName());
  }

  /**
   * Toggles between light and dark color schemes.
   */
  public static void toggleColorScheme() {
    ColorScheme current = getCurrentColorScheme();
    ColorScheme next = (current == ColorScheme.DARK) ? ColorScheme.LIGHT : ColorScheme.DARK;
    setColorScheme(next);
  }

  /**
   * Gets the currently active color scheme.
   *
   * @return The current color scheme, or SYSTEM as default
   */
  public static ColorScheme getCurrentColorScheme() {
    VaadinSession session = VaadinSession.getCurrent();
    if (session == null) {
      return ColorScheme.SYSTEM;
    }

    ColorScheme scheme = (ColorScheme) session.getAttribute(COLOR_SCHEME_SESSION_KEY);
    return scheme != null ? scheme : ColorScheme.SYSTEM;
  }

  /**
   * Checks if dark mode is currently active. Note: This may not be accurate when using SYSTEM color
   * scheme.
   *
   * @return true if dark mode is active
   */
  public static boolean isDarkMode() {
    return getCurrentColorScheme() == ColorScheme.DARK;
  }

  // =========================================================================
  // INITIALIZATION METHODS
  // =========================================================================

  /**
   * Initializes the theme system with default settings. Call this from your main application class
   * or a UIInitListener.
   *
   * @param defaultTheme       The default theme to use
   * @param defaultColorScheme The default color scheme to use
   */
  public static void initialize(Theme defaultTheme, ColorScheme defaultColorScheme) {
    VaadinSession session = VaadinSession.getCurrent();

    // Only initialize if not already set
    if (session.getAttribute(THEME_SESSION_KEY) == null) {
      switchTheme(defaultTheme);
    }

    if (session.getAttribute(COLOR_SCHEME_SESSION_KEY) == null) {
      setColorScheme(defaultColorScheme);
    }
  }

  /**
   * Initializes with Aura theme and system color scheme (auto).
   */
  public static void initializeWithDefaults() {
    initialize(Theme.AURA, ColorScheme.SYSTEM);
  }

  /**
   * Restores theme settings from session. Call this when a new UI is created to restore user's
   * theme preference.
   */
  public static void restoreFromSession() {
    Theme theme = getCurrentTheme();
    ColorScheme colorScheme = getCurrentColorScheme();

    // Re-apply stored settings
    switchTheme(theme);
    setColorScheme(colorScheme);
  }

  // =========================================================================
  // HELPER METHODS
  // =========================================================================

  /**
   * Removes the currently loaded theme stylesheet.
   */
  private static void removeExistingTheme(VaadinSession session) {
    // Remove main theme stylesheet
    Runnable themeRemover = (Runnable) session.getAttribute(THEME_REMOVER_SESSION_KEY);
    if (themeRemover != null) {
      themeRemover.run();
      session.setAttribute(THEME_REMOVER_SESSION_KEY, null);
    }

    // Remove custom styles
    Runnable customRemover = (Runnable) session.getAttribute(CUSTOM_STYLE_REMOVER_SESSION_KEY);
    if (customRemover != null) {
      customRemover.run();
      session.setAttribute(CUSTOM_STYLE_REMOVER_SESSION_KEY, null);
    }
  }

  /**
   * Adds a listener that will be notified when the theme changes.
   * <p>
   * Note: This is a placeholder for future implementation. Currently, components can check
   * getCurrentTheme() when needed.
   */
  @SuppressWarnings("unused")
  public static void addThemeChangeListener(Consumer<Theme> listener) {
    // This is a simplified implementation. For production, consider using
    // a proper event system or Vaadin's broadcast mechanism.
    // For now, components can check getCurrentTheme() when needed.
  }
}
