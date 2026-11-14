package com.example.app.ui.views;

import com.example.app.ui.security.SecurityService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;

/**
 * Landing page after login: a navigation drawer on the left, an account drawer on the right, each
 * shown/hidden by its own toggle button in the toolbar. WCAG 2.1 AAA: drawers are landmarks (nav /
 * aside), toggles report aria-expanded and are 44x44 px.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Home | Vaadin App")
@PermitAll
@StyleSheet("./styles/main-view.css")
public class MainView extends VerticalLayout {

  private static final String LEFT_DRAWER_ID = "main-left-drawer";
  private static final String RIGHT_DRAWER_ID = "main-right-drawer";
  private static final String ADMIN_ROLE = "ADMIN";
  /**
   * Must match the media query in main-view.css, below which drawers overlay the content.
   */
  private static final int OVERLAY_BREAKPOINT_PX = 768;

  private final SecurityService securityService;
  private final Nav leftDrawer;
  private final Button leftToggle;

  @Inject
  public MainView(SecurityService securityService) {
    this.securityService = securityService;

    setSizeFull();
    setPadding(false);
    setSpacing(false);
    addClassName("main-view");

    this.leftDrawer = createLeftDrawer();
    var rightDrawer = createRightDrawer();
    rightDrawer.setVisible(false);

    this.leftToggle = createDrawerToggle(this.leftDrawer, "Navigation", VaadinIcon.MENU);
    var toolbar = new HorizontalLayout(
        this.leftToggle,
        createDrawerToggle(rightDrawer, "Account panel", VaadinIcon.USER));
    toolbar.setWidthFull();
    toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
    toolbar.addClassNames("main-view__toolbar", LumoUtility.Padding.SMALL);

    var body = new Div(leftDrawer, createMainContent(), rightDrawer);
    body.addClassName("main-view__body");

    add(toolbar, body);
    setFlexGrow(1, body);
  }

  /**
   * On phones the open navigation drawer would cover the page, so start with it closed there.
   */
  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    // Collected by Vaadin during UI init; -1 if unknown, in which case the drawer stays open.
    var windowWidth = attachEvent.getUI().getPage().getExtendedClientDetails()
        .getWindowInnerWidth();
    if (windowWidth > 0 && windowWidth < OVERLAY_BREAKPOINT_PX) {
      setDrawerOpen(this.leftDrawer, this.leftToggle, false);
    }
  }

  /**
   * The accessible name stays fixed ("Toggle Navigation") while aria-expanded carries the state,
   * which is how screen readers expect a disclosure button to behave.
   */
  private Button createDrawerToggle(Component drawer, String drawerName, VaadinIcon icon) {
    var toggle = new Button(icon.create());
    toggle.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_LARGE,
        ButtonVariant.LUMO_CONTRAST);
    toggle.setAriaLabel("Toggle " + drawerName);
    toggle.setTooltipText("Show or hide " + drawerName.toLowerCase());
    toggle.addClassName("main-view__toggle");
    toggle.getElement().setAttribute("aria-controls", drawer.getId().orElseThrow());
    setDrawerOpen(drawer, toggle, drawer.isVisible());
    toggle.addClickListener(event -> setDrawerOpen(drawer, toggle, !drawer.isVisible()));
    return toggle;
  }

  private static void setDrawerOpen(Component drawer, Button toggle, boolean open) {
    drawer.setVisible(open);
    toggle.getElement().setAttribute("aria-expanded", String.valueOf(open));
  }

  private Nav createLeftDrawer() {
    var sideNav = new SideNav();
    sideNav.addItem(new SideNavItem("Home", MainView.class, VaadinIcon.HOME.create()));
    if (this.securityService.isCallerInRole(ADMIN_ROLE)) {
      sideNav.addItem(
          new SideNavItem("User Management", AdminView.class, VaadinIcon.USERS.create()));
    }

    var heading = drawerHeading("Navigation", "main-left-drawer-heading");
    var drawer = new Nav(heading, sideNav);
    drawer.setId(LEFT_DRAWER_ID);
    drawer.getElement().setAttribute("aria-labelledby", heading.getId().orElseThrow());
    styleDrawer(drawer, "main-view__drawer--left");
    return drawer;
  }

  private Aside createRightDrawer() {
    var username = this.securityService.getAuthenticatedUsername().orElse("Guest");
    var signedInAs = new Paragraph("Signed in as " + username);
    signedInAs.addClassNames(LumoUtility.TextColor.BODY, LumoUtility.Margin.NONE);

    var heading = drawerHeading("Account", "main-right-drawer-heading");
    var drawer = new Aside(heading, signedInAs);
    drawer.setId(RIGHT_DRAWER_ID);
    drawer.getElement().setAttribute("aria-labelledby", heading.getId().orElseThrow());
    styleDrawer(drawer, "main-view__drawer--right");
    return drawer;
  }

  private static H2 drawerHeading(String text, String id) {
    var heading = new H2(text);
    heading.setId(id);
    heading.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.Top.NONE,
        LumoUtility.Margin.Bottom.MEDIUM);
    return heading;
  }

  private static void styleDrawer(HasStyle drawer, String sideClassName) {
    drawer.addClassNames(
        "main-view__drawer",
        sideClassName,
        LumoUtility.Background.BASE,
        LumoUtility.Padding.MEDIUM,
        LumoUtility.BoxShadow.SMALL);
  }

  private Section createMainContent() {
    var title = new H1("Welcome to Vaadin 25 on Payara 7");
    title.addClassNames(LumoUtility.TextColor.HEADER, LumoUtility.Margin.Top.NONE,
        LumoUtility.Margin.Bottom.MEDIUM);

    var description = new Paragraph("""
        This application demonstrates Vaadin 25 running on Payara 7 \
        with Java 21 and Jakarta EE 11.""");
    description.addClassNames(LumoUtility.TextColor.BODY);

    var content = new Section(title, description);
    content.addClassNames(
        "main-view__content",
        LumoUtility.Background.BASE,
        LumoUtility.Padding.LARGE,
        LumoUtility.BorderRadius.MEDIUM,
        LumoUtility.BoxShadow.SMALL);
    return content;
  }
}
