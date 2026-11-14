package com.example.app.ui.views;

import com.example.app.application.user.UserAdministrationUseCase;
import com.example.app.application.user.UserView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import java.time.format.DateTimeFormatter;

/**
 * Admin view for user management. Only accessible to users with ADMIN role. WCAG 2.1 AAA
 * compliant.
 */
@Route(value = "admin", layout = MainLayout.class)
@PageTitle("Admin Panel | Vaadin App")
@RolesAllowed("ADMIN")
@StyleSheet("./styles/admin-view.css")
public class AdminView extends VerticalLayout {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd HH:mm");

  @Inject
  private UserAdministrationUseCase userAdministration;

  private Grid<UserView> userGrid;

  @PostConstruct
  public void init() {
    setupLayout();
    createHeader();
    createUserGrid();
    loadUsers();
  }

  private void setupLayout() {
    setSpacing(true);
    setPadding(true);
    setSizeFull();
  }

  private void createHeader() {
    H1 title = new H1("User Management");
    title.addClassNames(
        LumoUtility.TextColor.PRIMARY,
        LumoUtility.Margin.Bottom.MEDIUM);
    add(title);
  }

  private void createUserGrid() {
    userGrid = new Grid<>(UserView.class, false);
    userGrid.addThemeVariants(
        GridVariant.LUMO_ROW_STRIPES,
        GridVariant.LUMO_WRAP_CELL_CONTENT);
    userGrid.setSizeFull();

    userGrid.addColumn(UserView::username)
        .setHeader("Username")
        .setSortable(true)
        .setAutoWidth(true);

    userGrid.addColumn(UserView::email)
        .setHeader("Email")
        .setSortable(true)
        .setAutoWidth(true);

    userGrid.addColumn(UserView::roleName)
        .setHeader("Role")
        .setSortable(true)
        .setAutoWidth(true);

    userGrid.addColumn(new ComponentRenderer<>(user -> {
          Span badge = new Span(user.active() ? "Active" : "Inactive");
          badge.getElement().getThemeList().add(
              user.active() ? "badge success" : "badge error");
          return badge;
        }))
        .setHeader("Status")
        .setAutoWidth(true);

    userGrid.addColumn(
            user -> user.createdAt() != null ? user.createdAt().format(DATE_FORMATTER) : "")
        .setHeader("Created")
        .setSortable(true)
        .setAutoWidth(true);

    userGrid.addComponentColumn(user -> {
          Button lockButton = new Button(
              user.accountLocked() ? "Unlock" : "Lock",
              event -> toggleLockStatus(user));
          lockButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
          lockButton
              .setAriaLabel(
                  (user.accountLocked() ? "Unlock" : "Lock") + " account for " + user.username());
          return lockButton;
        })
        .setHeader("Actions")
        .setAutoWidth(true);

    userGrid.setAriaLabel("User management grid");

    add(userGrid);
  }

  private void loadUsers() {
    userGrid.setItems(userAdministration.listUsers());
  }

  private void toggleLockStatus(UserView user) {
    userAdministration.setAccountLocked(user.id(), !user.accountLocked());
    loadUsers();
  }
}
