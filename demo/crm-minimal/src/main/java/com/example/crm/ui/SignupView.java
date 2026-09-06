package com.example.crm.ui;

import com.example.crm.service.CrmSignupHandler;
import com.holonplatform.multitenant.TenantDetails;
import com.holonplatform.vaadin.flow.components.Components;
import com.holonplatform.vaadin.flow.components.Input;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("signup")
@PageTitle("Create your workspace")
@AnonymousAllowed
public class SignupView extends VerticalLayout {

    private final CrmSignupHandler signupHandler;

    // Step 1 — identity
    private final Input<String> companyNameField = Components.input.string()
            .label("Company name")
            .build();
    private final Input<String> emailField = Components.input.string()
            .label("Your email")
            .build();
    private final Input<String> passwordField = Components.input.password()
            .label("Password")
            .build();
    private final Input<String> confirmPasswordField = Components.input.password()
            .label("Confirm password")
            .build();

    // Step 2 — branding (optional)
    private final Input<String> logoUrlField = Components.input.string()
            .label("Logo URL")
            .build();
    private final Input<String> primaryColorField = Components.input.string()
            .label("Primary color")
            .build();
    private final Input<String> fontField = Components.input.string()
            .label("Font")
            .build();

    // Step 3 — regional
    private final Input<String> localeField = Components.input.string()
            .label("Locale")
            .build();
    private final Input<String> timezoneField = Components.input.string()
            .label("Timezone")
            .build();
    private final Input<String> currencyField = Components.input.string()
            .label("Currency")
            .build();

    // Step 4 — industry / review
    private final Input<String> industryField = Components.input.string()
            .label("Industry")
            .build();

    private final Div errorMessage = new Div();
    private final Div[] steps = new Div[4];
    private int currentStep = 0;

    public SignupView(CrmSignupHandler signupHandler) {
        this.signupHandler = signupHandler;

        localeField.setValue("en-US");
        timezoneField.setValue("UTC");
        currencyField.setValue("USD");

        errorMessage.getStyle()
                .set("color", "var(--lumo-error-text-color)");
        errorMessage.setVisible(false);

        steps[0] = new Div(new H2("1. Your account"),
                companyNameField.getComponent(), emailField.getComponent(),
                passwordField.getComponent(), confirmPasswordField.getComponent());

        steps[1] = new Div(new H2("2. Branding (optional)"),
                logoUrlField.getComponent(), primaryColorField.getComponent(), fontField.getComponent());

        steps[2] = new Div(new H2("3. Regional settings"),
                localeField.getComponent(), timezoneField.getComponent(), currencyField.getComponent());

        steps[3] = new Div(new H2("4. Industry"), industryField.getComponent());

        backButton = Components.button()
                .text("Back")
                .withClickListener(e -> goTo(currentStep - 1))
                .build();
        nextButton = Components.button()
                .text("Next")
                .withClickListener(e -> next())
                .build();
        createButton = Components.button()
                .text("Create account")
                .withClickListener(e -> submit())
                .build();
        var backToLoginButton = Components.button()
                .text("Already have an account? Sign in")
                .withClickListener(e -> UI.getCurrent()
                        .navigate("login"))
                .build();

        add(new H1("Create your workspace"), errorMessage,
                steps[0], steps[1], steps[2], steps[3],
                this.backButton, this.nextButton, this.createButton,
                backToLoginButton);

        setAlignItems(Alignment.CENTER);
        goTo(0);
    }

    private final com.vaadin.flow.component.Component backButton;
    private final com.vaadin.flow.component.Component nextButton;
    private final com.vaadin.flow.component.Component createButton;

    private void goTo(int step) {
        if (step < 0 || step >= steps.length) {
            return;
        }
        currentStep = step;
        for (int i = 0; i < steps.length; i++) {
            steps[i].setVisible(i == currentStep);
        }
        backButton.setVisible(currentStep > 0);
        nextButton.setVisible(currentStep < steps.length - 1);
        createButton.setVisible(currentStep == steps.length - 1);
        errorMessage.setVisible(false);
    }

    private void next() {
        if (currentStep == 0 && !validateStep1()) {
            return;
        }
        goTo(currentStep + 1);
    }

    private boolean validateStep1() {
        if (isBlank(companyNameField.getValue()) || isBlank(emailField.getValue())
                || isBlank(passwordField.getValue())) {
            showError("Company name, email and password are required.");
            return false;
        }
        if (!passwordField.getValue()
                .equals(confirmPasswordField.getValue())) {
            showError("Passwords do not match.");
            return false;
        }
        return true;
    }

    private void submit() {
        if (!validateStep1()) {
            goTo(0);
            return;
        }
        try {
            TenantDetails tenant = signupHandler.completeWizard(
                    companyNameField.getValue(), emailField.getValue(), passwordField.getValue(),
                    logoUrlField.getValue(), primaryColorField.getValue(), fontField.getValue(),
                    localeField.getValue(), timezoneField.getValue(), currencyField.getValue(),
                    industryField.getValue());

            Notification.show("Workspace '" + tenant.name() + "' created! Please sign in.",
                    4000, Notification.Position.MIDDLE);
            UI.getCurrent()
                    .navigate("login");
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(SignupView.class)
                    .error("Signup failed", e);
            showError("Could not create the account: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisible(true);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}