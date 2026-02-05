package com.example.vaadin.views;

import com.example.vaadin.entities.User;
import com.example.vaadin.security.SecurityService;
import com.example.vaadin.services.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.component.dependency.StyleSheet;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.time.format.DateTimeFormatter;

/**
 * Admin view for user management.
 * Only accessible to users with ADMIN role.
 * WCAG 2.1 AAA compliant.
 */
@Route("admin")
@PageTitle("Admin Panel | Vaadin App")
@RolesAllowed("ADMIN")
@StyleSheet("./styles/admin-view.css")
public class AdminView extends AppLayout {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject
    private UserService userService;

    @Inject
    public AdminView(SecurityService securityService) {
        super(securityService);
    }

    private Grid<User> userGrid;

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
        userGrid = new Grid<>(User.class, false);
        userGrid.addThemeVariants(
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_WRAP_CELL_CONTENT);
        userGrid.setSizeFull();

        userGrid.addColumn(User::getUsername)
                .setHeader("Username")
                .setSortable(true)
                .setAutoWidth(true);

        userGrid.addColumn(User::getEmail)
                .setHeader("Email")
                .setSortable(true)
                .setAutoWidth(true);

        userGrid.addColumn(user -> user.getRole().getDisplayName())
                .setHeader("Role")
                .setSortable(true)
                .setAutoWidth(true);

        userGrid.addColumn(new ComponentRenderer<>(user -> {
            Span badge = new Span(user.isActive() ? "Active" : "Inactive");
            badge.getElement().getThemeList().add(
                    user.isActive() ? "badge success" : "badge error");
            return badge;
        }))
                .setHeader("Status")
                .setAutoWidth(true);

        userGrid.addColumn(user -> user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FORMATTER) : "")
                .setHeader("Created")
                .setSortable(true)
                .setAutoWidth(true);

        userGrid.addComponentColumn(user -> {
            Button lockButton = new Button(
                    user.isAccountLocked() ? "Unlock" : "Lock",
                    event -> toggleLockStatus(user));
            lockButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            lockButton
                    .setAriaLabel((user.isAccountLocked() ? "Unlock" : "Lock") + " account for " + user.getUsername());
            return lockButton;
        })
                .setHeader("Actions")
                .setAutoWidth(true);

        userGrid.setAriaLabel("User management grid");

        add(userGrid);
    }

    private void loadUsers() {
        userGrid.setItems(userService.findAllUsers());
    }

    private void toggleLockStatus(User user) {
        userService.setAccountLocked(user.getId(), !user.isAccountLocked());
        loadUsers();
    }
}
