package com.example.vaadin.views;

import com.example.vaadin.security.SecurityService;
import com.example.vaadin.services.GreetingService;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;

/**
 * Main view - accessible to authenticated users.
 * WCAG 2.1 AAA compliant.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Home | Vaadin App")
@PermitAll
public class MainView extends VerticalLayout {

    @Inject
    private SecurityService securityService;

    @Inject
    private GreetingService greetingService;

    @PostConstruct
    public void init() {
        createContent();
    }

    private void createContent() {
        H1 title = new H1("Welcome to Vaadin 25 on Payara 7");
        title.addClassNames(
                LumoUtility.TextColor.PRIMARY,
                LumoUtility.Margin.Bottom.MEDIUM);

        Paragraph description = new Paragraph(
                "This application demonstrates Vaadin 25 running on GlassFish Payara 7 " +
                        "with Java 21 and Jakarta EE 10.");
        description.addClassNames(LumoUtility.TextColor.BODY);

        String username = securityService.getAuthenticatedUsername().orElse("Developer");

        add(title, description );
    }
}
