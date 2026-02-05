package com.example.vaadin.views;

import com.example.vaadin.security.SecurityService;
import com.example.vaadin.services.GreetingService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
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
@Route("")
@PageTitle("Home | Vaadin App")
@PermitAll
public class MainView extends AppLayout {

    private final GreetingService greetingService;

    @Inject
    public MainView(SecurityService securityService, GreetingService greetingService) {
        super(securityService);
        this.greetingService = greetingService;
    }

    @PostConstruct
    public void init() {
        createContent();
    }

    private void createContent() {
        H1 title = new H1("Welcome to Vaadin 25 on Payara 6");
        title.addClassNames(
                LumoUtility.TextColor.PRIMARY,
                LumoUtility.Margin.Bottom.MEDIUM);

        Paragraph description = new Paragraph(
                "This application demonstrates Vaadin 25 running on GlassFish Payara 6 " +
                        "with Java 21 and Jakarta EE 10.");
        description.addClassNames(LumoUtility.TextColor.BODY);

        String username = getSecurityService().getAuthenticatedUsername().orElse("Developer");
        Button greetButton = new Button("Greet Me", event -> {
            String greeting = greetingService.getGreeting(username);
            Notification notification = Notification.show(greeting, 3000, Notification.Position.MIDDLE);
            notification.getElement().setAttribute("role", "status");
            notification.getElement().setAttribute("aria-live", "polite");
        });
        greetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        setContent(title, description, greetButton);
    }
}
