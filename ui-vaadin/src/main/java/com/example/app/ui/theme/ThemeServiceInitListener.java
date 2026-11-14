package com.example.app.ui.theme;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service init listener that restores theme preferences when a new UI is created. This ensures that
 * user's theme selection persists across page refreshes and sessions.
 *
 * @author Your Name
 * @version 1.0
 * @since Vaadin 25
 */
@ApplicationScoped
public class ThemeServiceInitListener implements VaadinServiceInitListener {

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.getSource().addUIInitListener(uiInitEvent -> {
      // Restore theme settings from session when UI is initialized
      ThemeSwitcher.restoreFromSession();
    });
  }
}
